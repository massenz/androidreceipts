// Copyright Infinite Bandwidth ltd (c) 2010. All rights reserved.
// Created 6 Oct 2010, by marco
package com.alertavert.receiptscan.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * <h1>Money</h1>
 *
 * <p>Value class to represent a monetary amount in any arbitrary currency.
 * Without trying to be too sophisticated, a "monetary amount" is defined by
 * three attributes: integer part, decimal part (cents, pennies, etc.) and a 
 * currency symbol (an uppercase three-letter abbreviation, according to ISO-4217).
 *
 * @author <a href='mailto:m.massenzio@gmail.com'>Marco Massenzio</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
@Embeddable
public class Money implements Serializable {
  @Column private long intValue;
  @Column private int decValue;
  @Column(length = 3) private char[] cur = new char[3];


  /** Provided here only to please GWT serialization policy - DON'T USE, won't do you any good */
  public Money() {
  }
  
  /**
   * Initializes a monetary value, enforcing consistency rules: both the integer and fractional
   * parts MUST be positive, and the fractional value MUST be between 0-99.
   * 
   * @param intVal the integral part of the cash amount, >= 0
   * @param decVal the fractional amount (eg, cents) [0..99]
   * @param currency a three-char ISO-4217-compliant currency code
   */
  public Money(long intVal, int decVal, CharSequence currency) {
    if ((intVal < 0) || (decVal < 0) || (decVal > 99))
      throw new IllegalArgumentException("Values must be positive or zero, and the fractional" +
      		" part must be less than or equal to 99.\nintVal=" + intVal + 
      		"\ndecVal=" + decVal);
    this.intValue = intVal;
    this.decValue = decVal;
    String fmtCur = currency.toString().trim().toUpperCase().substring(0, cur.length);
    for (int i = 0; i < cur.length; ++i) {
      cur[i] = fmtCur.charAt(i);
    }
  }
  
  public long getIntValue() {
    return intValue;
  }
  
  /** @return a floating point representation of this monetary value */
  public float getFloatValue() {
    double d = (double) intValue * 100.0;
    double rd = Math.floor(d + decValue);
    return (float)(rd / 100); 
  }
  
  public static Money parse(float money, CharSequence currency) {
    long parseInt = Math.round(Math.floor((double)money));
    int cents = Math.round((money - parseInt) * 100.0f);
    return new Money(parseInt, cents, currency);
  }

  @Override
  public String toString() {
    return "" + intValue + "." + (decValue < 10 ? "0" : "") + decValue;
  }
  
  public String toStringWithCurrency() {
    return new StringBuilder()
        .append(cur)
        .append(toString())
        .toString();
  }

  /**
   * @return
   */
  public int getFraction() {
    return decValue;
  }

  /**
   * @return the currency for this monetary amount
   */ 
  public String getCurrency() {
    return new String(cur);
  }
}
