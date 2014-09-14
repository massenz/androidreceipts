// Copyright Infinite Bandwidth ltd (c) 2010. All rights reserved.
// Created 11 Oct 2010, by marco
package com.alertavert.android.applications.receipts;


import java.io.Serializable;
import java.util.Collection;

import com.alertavert.receiptscan.model.Receipt;


/**
 * <h1>ReceiptsFormatter</h1>
 *
 * <p>TODO(marco) Insert class description here
 *
 * <h4>All rights reserved Infinite Bandwidth ltd (c) 2010</h4><br>
 * @author <a href='mailto:m.massenzio@gmail.com'>Marco Massenzio</a>
 * @version 1.0
 */
public interface ReceiptsFormatter extends Serializable {
  public abstract String format(Collection<Receipt> receipts);
}
