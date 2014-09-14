// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts.connectivity;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.EmailParseException;
import android.net.MailTo;
import android.net.Uri;
import android.util.Log;

import com.alertavert.android.applications.receipts.ControllerActivity;
import com.alertavert.android.applications.receipts.R;
import com.alertavert.android.applications.receipts.ReceiptsFormatter;
import com.alertavert.receiptscan.model.Receipt;


/**
 * <h1>MailSender</h1>
 * <p>
 * A simple email sender that uses the phone's ACTION_SEND_MULTIPLE Intent action to send an email
 * with multiple attachments
 * 
 * @author m.massenzio@gmail.com (Marco Massenzio)
 */
public class MailSender implements Sender {

  /** Regular expression to validate an email address */
  private final static String REGEX = "[\\w/.]+@[\\w/.]+[a-z]{2}|com|net|org";

  private MailTo destination;

  Activity activity;

  private ReceiptsFormatter receiptsFormatter;
  
  private List<ReceiptsSenderListener> listeners = new ArrayList<ReceiptsSenderListener>();

  private String reason = "";

  /**
   * As the constructor can fail in several ways, we need to flag whether this sender is in a valid
   * state
   */
  private boolean isValid = false;

  public ReceiptsFormatter getReceiptsFormatter() {
    return receiptsFormatter;
  }

  public void setReceiptsFormatter(ReceiptsFormatter receiptsFormatter) {
    this.receiptsFormatter = receiptsFormatter;
  }

  public MailSender(String email, Activity activity) {
    this.activity = activity;
    if ((email == null) || (email.length() == 0)) {
      setReason(R.string.sender_email_empty);
      return;
    }
    String mailUri = MailTo.MAILTO_SCHEME + email;

    if (MailTo.isMailTo(mailUri)) {
      try {
        if (!isEmailValid(email)) {
          Log.e(ControllerActivity.TAG, reason);
          setReason(R.string.sender_email_invalid, email);
          return;
        }
        destination = MailTo.parse(mailUri);
      } catch (EmailParseException ex) {
        // constructors should not throw, so we catch and leave the object in a known state
        Log.e(MailSender.class.getName(), "Could not parse into a valid email address: " + email, ex);
        setReason(R.string.sender_email_cannot_parse, email);
        destination = null;
        return;
      }
    }
    isValid = true;
  }

  private boolean isEmailValid(String email) {
    return email.matches(REGEX);
  }

  /**
   * Sets the destination address from a simple email address ({@code your.name@domain.com})
   * <p>
   * The default {@link MailTo#parse(String)} simply checks for an initial {@code mailto:} protocol
   * scheme (yes, really) but not for correctness: this method implements a very basic correctness
   * check (presence of an '@', no spaces, etc.) using a RegEx, and throws an EmailParseException if
   * that fails; however, clients are strongly encouraged to check that the passed in String
   * represents a valid email address, prior to calling this method.
   * 
   * @param email a simple destination address
   * @throws EmailParseException if {@code email} is not a vaild email address
   */
  public void setDestinationEmail(String email) throws EmailParseException {
    if (!isEmailValid(email)) {
      isValid = false;
      setReason(R.string.sender_email_invalid, email);
      // TODO(marco): fix the nonsense in android.net.ParseException
      throw new IllegalArgumentException(reason);
    }
    this.destination = MailTo.parse(MailTo.MAILTO_SCHEME + email);
  }

  /**
   * Sends the {@code receipts} to the destination address as set previously (either at
   * construction, or subsequently using either {@link #setDestinationEmail(String)}, or
   * {@link #setDestination(java.net.URI)}).
   * <p>
   * This uses the {@link Intent#ACTION_SEND_MULTIPLE ACTION_SEND_MULTIPLE} Intent action, which
   * will fire up the email activity and send the receipts as a collection of JPG file attachments
   * 
   * @see Sender#send(java.util.Collection)
   */
  @Override
  public boolean send(Collection<Receipt> receipts) {
    boolean abort = false;

    if (!isValid) {
      Log.e(ControllerActivity.TAG, "Sender in invalid state (" + reason + ")");
      abort = true;
    }
    if (activity == null) {
      Log.e(ControllerActivity.TAG, "Cannot use sender with a null Activity");
      abort = true;
    }
    if (destination == null) {
      setReason(R.string.sender_email_empty);
      Log.e(ControllerActivity.TAG, reason);
      abort = true;
    }
    if (abort) {
      for (ReceiptsSenderListener listener : listeners) {
        listener.onFailure(this, receipts, converDestToUri(), null);
      }
      return false;
    }
    
    Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);

    i.setType("image/jpg");
    Log.d(ControllerActivity.TAG, "Sending email to " + destination.getTo());
    i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { destination.getTo() });
    String subject = activity.getResources().getString(R.string.sender_subject);

    i.putExtra(Intent.EXTRA_SUBJECT, subject);
    if (receiptsFormatter != null) {
      i.putExtra(android.content.Intent.EXTRA_TEXT, receiptsFormatter.format(receipts));
    } else {
      String body = activity.getResources().getString(R.string.sender_body, receipts.size());

      i.putExtra(android.content.Intent.EXTRA_TEXT, body);
    }
    ArrayList<Uri> uris = new ArrayList<Uri>();

    if (!receipts.isEmpty()) {
      Iterator<Receipt> iter = receipts.iterator();

      while (iter.hasNext()) {
        Receipt r = iter.next();

        if (r.getImageUri() != null) {
          Log.d(ControllerActivity.TAG, "Attaching: " + r.getName() + " [" + r.getImageUri() + "]");
          uris.add(convertToAndroidUri(r.getImageUri()));
        }
      }
      Log.d(ControllerActivity.TAG, "Attaching " + uris.size() + " files to email");
      i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);      
      for (ReceiptsSenderListener listener : listeners) {
        listener.onSend(this, receipts);
      }
      activity.startActivity(i/* Intent.createChooser(i, "Sending receipts....") */);
      reason = "";
      for (ReceiptsSenderListener listener : listeners) {
        listener.onSuccess(this, receipts, converDestToUri());
      }
      return true;
    } else {
      setReason(R.string.sender_no_receipts);
      Log.d(ControllerActivity.TAG, "NO receipts available to attach, not sending email");
      for (ReceiptsSenderListener listener : listeners) {
        listener.onFailure(this, receipts, converDestToUri(), null);
      }
      return false;
    }
  }
  
  private URI converDestToUri() {
    try {
      return new URI(MailTo.MAILTO_SCHEME + destination);
    } catch (URISyntaxException ex) {
      return null;
    }
  }

  /**
   * @param imageUri
   * @return
   */
  protected Uri convertToAndroidUri(URI imageUri) {
    return Uri.parse(imageUri.toString());
  }

  /**
   * Use this method only with an {@code absolute URI} of the form:
   * 
   * <pre>
   *   mailto:user@domain.com
   * </pre>
   * 
   * Anything else will cause an exception to be thrown (see also <a
   * href="http://java.sun.com/javase/6/docs/api/java/net/URI.html">Java documentation</a> for the
   * URI class.)
   * 
   * <p><strong>Note</strong>: that a URI of this form is <strong>not</strong> a URL, but instead a
   * <strong>URN</strong> and will not be further parsed.
   * 
   * @see Sender#setDestination(java.net.URI) Sender.setDestination()
   * @see java.net.URI
   */
  @Override
  public void setDestination(URI dest) {
    // Awkward fix to cope with inconsistent use of ':' in Dalvik
    // see also http://b/issue?id=2617392
    if (((dest.getScheme() + ":").equals(MailTo.MAILTO_SCHEME))
        && (isEmailValid(dest.getSchemeSpecificPart()))) {
      setDestinationEmail(dest.getSchemeSpecificPart());
    } else {
      throw new IllegalArgumentException("URI is not a valid email URN: " + dest.toString());
    }
  }

  @Override
  public boolean setSenderOption(String name, String value) {
    // TODO(marco): implement this method, if required by an email sender?
    return false;
  }

  /**
   * @return the email destination for this sender
   */
  public MailTo getDestination() {
    return destination;
  }

  /*
   * (non-Javadoc)
   * @see com.alertavert.android.applications.receipts.connectivity.Sender#getFailureReason()
   */
  @Override
  public String getFailureReason() {
    return reason;
  }
  
  /**
   * Convenience method to build a failure reason
   * <pre>[String Resource(id)] + suffix + email</pre>
   * 
   * @param id an optional string resource ID (or -1, to ignore)
   * @param suffix an optional (but non-null) sentence between the string res and the email
   * @param email appended at the end of the reason (leave empty if no email)
   */
  private void setReason(int id, Object... args) {
    reason = (id > -1 ? activity.getResources().getString(id, args) : "");
  }

  /* (non-Javadoc)
   * @see com.alertavert.android.applications.receipts.connectivity.Sender#addSenderListener(com.alertavert.android.applications.receipts.connectivity.ReceiptsSenderListener)
   */
  @Override
  public void addSenderListener(ReceiptsSenderListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /* (non-Javadoc)
   * @see com.alertavert.android.applications.receipts.connectivity.Sender#removeSenderListener(com.alertavert.android.applications.receipts.connectivity.ReceiptsSenderListener)
   */
  @Override
  public void removeSenderListener(ReceiptsSenderListener listener) {
    listeners.remove(listener);
  }
}
