/**
 * 
 */
package com.alertavert.android.applications.receipts.connectivity;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import com.alertavert.android.applications.receipts.ControllerActivity;
import com.alertavert.receipts.model.proto.ReceiptsProtos;
import com.alertavert.receipts.model.proto.ReceiptsProtos.ReceiptProto;
import com.alertavert.receipts.model.proto.ReceiptsProtos.Token;
import com.alertavert.receiptscan.model.Receipt;
import com.google.protobuf.ByteString;


/**
 * A sender using HTTP(S) as the transport protocol, communicating with the
 * server whose URL has been set in {@link #destination}.
 * 
 * <p>Additional request properties can be set using {@link #setSenderOption(String, String)}
 * which will be sent for ALL requests (worth noting that the {@link Sender} API does not
 * allow for removal of options once they are set.
 * 
 * @author Marco Massenzio (m.massenzio@gmail.com)
 *
 */
public class HttpSender implements Sender {

  private static final String USERAGENT = "AndroidReceipts-server-alertavert_1.0.0;android/receipts";
  private URL destination;
  private String reason;
  private Map<String, String> requestProperties = new HashMap<String, String>();
  private Set<ReceiptsSenderListener> listeners = new HashSet<ReceiptsSenderListener>();
  
  public HttpSender() {
    setSenderOption("User-Agent", USERAGENT);
  }
  
  /* (non-Javadoc)
   * @see com.alertavert.android.applications.receipts.connectivity.Sender#send(java.util.Collection)
   */
  @Override
  public boolean send(Collection<Receipt> receipts) {
    if (receipts.isEmpty()) {
      return false;
    }
    BufferedOutputStream os = null;
    HttpURLConnection connection = null;

    for (ReceiptsSenderListener listener : listeners) {
      listener.onSend(this, receipts);
    }
    try {
      connection = (HttpURLConnection) destination.openConnection();
      if (connection == null) {
        reason = "Could connect to server " + destination;
        return false;
      }
      // Uses POST
      connection.setDoOutput(true);
      // get the default chunk length
      connection.setChunkedStreamingMode(0);
      for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
        connection.setRequestProperty(entry.getKey(), entry.getValue());
      }
      os = new BufferedOutputStream(connection.getOutputStream());
      byte[] data = wrapIntoProtoPayload(receipts).toByteArray();

      Log.d(ControllerActivity.TAG, "Proto serialized into " + data.length + " bytes");
      os.write(data);
      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        notifyListeners(true, receipts, null);
        return true;
      } else {
        notifyListeners(false, receipts, null);
        Log.e(ControllerActivity.TAG, "Error uploading receipts: " + connection.getResponseMessage());
        return false;
      }
    } catch (IOException ex) {
      Log.e(ControllerActivity.TAG, "HttpSender::Could not upload receipts to the server", ex);
      notifyListeners(false, receipts, ex);
    } finally {
      try {
        if (os != null) {
          os.close();
        }
        if (connection != null) {
          connection.disconnect();
        }
      } catch (IOException stupidestExceptionEver) {// just ignore the little fucker
      }
    }
    return false;
  }
  
  private synchronized void notifyListeners(boolean success, Collection<Receipt> receipts, Throwable ex) {
    for (ReceiptsSenderListener listener : listeners) {
      try {
        if (success) {
          Log.d(ControllerActivity.TAG, "Receipts successfully uploaded");
          listener.onSuccess(this, receipts, destination.toURI());          
        } else {
          listener.onFailure(this, receipts, destination.toURI(), ex);
        }
      } catch (URISyntaxException e) {
        Log.e(ControllerActivity.TAG, "Could not convert " + destination.toExternalForm(), ex);
      } 
    }
  }
  
  private ReceiptsProtos.ReceiptsPayload wrapIntoProtoPayload(Collection<Receipt> receipts)
    throws IOException {
    ReceiptsProtos.ReceiptsPayload.Builder builder = ReceiptsProtos.ReceiptsPayload.newBuilder();

    // TODO(marco): obtain these values from the SessionManager or a SecAuth layer
    builder.setToken(getSessionToken());
    for (Receipt r : receipts) {
      builder.addReceipts(wrapIntoReceiptProto(r));
      Log.d(ControllerActivity.TAG, "Adding receipt " + r.getName());
    }
    return builder.build();
  }
  
  // TODO(marco): should access the app's SecurityManager to retrieve the user's credentials
  private Token getSessionToken() {
    return Token.newBuilder().setToken("abcdef123456789").setUsername("AnUser").setExpiryTimestamp(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(3600L, TimeUnit.SECONDS)).build();
  }

  private ReceiptProto wrapIntoReceiptProto(Receipt r) throws IOException {
    ReceiptProto.Builder builder = ReceiptProto.newBuilder();

    if (r.getId() != 0) {
      builder.setId("" + r.getId());
    }
    builder.setName(r.getName());
    builder.setAmount(r.getAmount().getFloatValue());
    if (r.getAmount().getCurrency().length() == 0) {// TODO(marco) extract the default currency for the locale 
    } else {
      builder.setCurrency(r.getAmount().getCurrency());
    }
    builder.setMerchant(r.getMerchant());
    if (r.getNotes().length() > 0) {
      builder.setNotes(r.getNotes());
    }
    // Extract the image data and wrap into the protocol buffer for serialization
    byte[] buf = new byte[1024];
    InputStream is = new BufferedInputStream(new FileInputStream(r.getImageUri().getPath()));
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    int numBytesRead;

    while ((numBytesRead = is.read(buf)) != -1) {
      bos.write(buf, 0, numBytesRead);
    }
    is.close();
    ByteString image = ByteString.copyFrom(bos.toByteArray());

    builder.setImage(image);
    return builder.build();
  }

  /* (non-Javadoc)
   * @see com.alertavert.android.applications.receipts.connectivity.Sender#setDestination(java.net.URI)
   */
  @Override
  public void setDestination(URI dest) {
    if (!dest.getScheme().equals("http")) {
      throw new IllegalArgumentException("Should always be a http:// URL: " + dest.toString());
    }
    try {
      destination = dest.toURL();
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Invalid URL: " + dest.toString(), ex);
    }
  }

  /**
   * This call always succeeds, adding the required option to the list of
   * request properties that will be set <strong>each time</strong> a send request is
   * made to the server.
   *  
   * {@inheritDoc}
   */
  @Override
  public boolean setSenderOption(String name, String value) {
    requestProperties.put(name, value);
    return true;
  }

  /* (non-Javadoc)
   * @see com.alertavert.android.applications.receipts.connectivity.Sender#getFailureReason()
   */
  @Override
  public String getFailureReason() {
    return reason;
  }

  /* (non-Javadoc)
   * @see com.alertavert.android.applications.receipts.connectivity.Sender#addSenderListener(com.alertavert.android.applications.receipts.connectivity.ReceiptsSenderListener)
   */
  @Override
  public synchronized void addSenderListener(ReceiptsSenderListener listener) {
    listeners.add(listener);
  }

  /* (non-Javadoc)
   * @see com.alertavert.android.applications.receipts.connectivity.Sender#removeSenderListener(com.alertavert.android.applications.receipts.connectivity.ReceiptsSenderListener)
   */
  @Override
  public synchronized void removeSenderListener(ReceiptsSenderListener listener) {
    listeners.remove(listener);
  }
}
