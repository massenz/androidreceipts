// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.receiptscan.model;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author m.massenzio@gmail.com (Marco Massenzio)
 */
@Entity
public class Receipt implements Serializable {
  /**
   * Version ID for Serializable
   */
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE)
  int id;
  
  @Column
  String name = "";
  
  @Temporal(TemporalType.DATE)
  Date timestamp = new Date();
  
  @Embedded
  Money amount = new Money(0, 0, "USD");
  
  @Column
  String merchant = "";
  
  @Column
  String notes = "";
  
  /**
   * The receipt's image location, encoded as a RFC 2396 URI
   * <p>
   * Please note this is a {@link com.alertavert.receiptscan.jre.java.net.URI} object and does not use the 
   * {@link android.net.Uri} class, so as to be compatible with server-side components.
   * <p>
   * This is marked as {@code transient} as we do not need to stream it across application's
   * layers: it only has meaning locally on the client.
   * 
   * // TODO (marco) verify that this is generally true, may limit extensibility of the app
   */
  transient URI imageUri;

  /** 
   * Use this default constructor to get an empty instance, then populate using the setters.
   * Upon creation, a Receipt is safe to use (all values initialized to default, mostly empty, 
   * values) but completely devoid of meaning.
   * <p>
   * The alternative (either an all-encompassing uber-constructor, a Builder pattern, or a series
   * of increasingly complex constructors) would have only made this class' usage messier, and
   * I have resisted the temptation to do so.
   * <p>
   * Just to be extra clear: <strong>do not use this class as constructed</strong>, but rather
   * initialize its member to meaningful values.
   */
  public Receipt() {
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public Money getAmount() {
    return amount;
  }
  
  public void setAmount(Money amount) {
    this.amount = amount;
  }

  public String getNotes() {
    return notes;
  }
  
  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getMerchant() {
    return merchant;
  }

  public void setMerchant(String merchant) {
    this.merchant = merchant;
  }

  public URI getImageUri() {
    return imageUri;
  }
  
  public void setImageUri(URI imageUri) {
    this.imageUri = imageUri;
  }
  
  @Override
  public String toString() {
    return getName() + " [" + getId() + "] :: " + getMerchant() + " for " + 
        (getAmount() != null ? getAmount().toStringWithCurrency() : "0.00") + 
        (getNotes() != null ? "\n\"" + getNotes() + "\"\n" : "") +
        (getImageUri() != null ? getImageUri().getPath() : "[no file]");
  }
}
