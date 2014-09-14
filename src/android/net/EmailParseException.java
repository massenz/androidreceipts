// Copyright AlertAvert.com (c) 2010. All rights reserved.

package android.net;


/**
 * <h1>EmailParseException</h1>
 * <p>
 * {@link android.net.ParseException} is badly screwed: for some reason the
 * constructor is package-visible only, and also does not allow one to set a 
 * reason ({@code detail} in this class's {@link #EmailParseException(String) constructor}).
 * <p>
 * Unfortunately, the {@link MailTo#parse(String) parse()} method in {@code MailTo}
 * does not check for mail address correctness, but only for the presence of a 
 * {@code mailto:} protocol; for consistency, we prefer to throw only a single type of
 * exception, but also avoid having to have a 'castle of catch-n-rethrow'.
 * 
 * TODO (mmassenzio) file a patch with the Android team
 *
 * <h3>Copyright Google (c) 2010. All rights reserved.</h3>
 *
 * @author m.massenzio@gmail.com (Marco Massenzio)
 *
 */
public class EmailParseException extends ParseException {

  private static final long serialVersionUID = 1L;

  public EmailParseException(String detail) {
    this.response = detail;
  }
	
  public EmailParseException(String detail, Throwable th) {
    if (th instanceof ParseException) {
      this.response = detail + "  Original response was: " + ((ParseException) th).response;
    }
  }
}
