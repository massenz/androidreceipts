// All rights reserved AlertAvert.com (c) 2011

package com.alertavert.receiptscan.model.proto.adapters;

/**
 * <h1>IAdaptable</h1>
 *
 * <p> This is a simple interface to define an adapter from class F to class T
 * 
 * TODO write better class description
 *
 * @author Marco Massenzio (m.massenzio@gmail.com)
 */
public interface IAdaptable<F, T> {
  public T adapt(F adaptee);
}
