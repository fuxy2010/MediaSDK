package com.nercms.schedule.sip.engine.sipua.ui;

/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import com.nercms.schedule.misc.AppManager;
import com.nercms.schedule.sip.engine.sipua.SP;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class ChangeAccount extends Activity {

	public static int getPref(Context context)
	{
		return SP.get(context, SP.PREF_ACCOUNT, SP.DEFAULT_ACCOUNT);
	}
		
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		AppManager.get_instance().add_activity(this);
		
		super.onCreate(savedInstanceState);
		
		SP.set(this, SP.PREF_ACCOUNT, SipdroidReceiver.engine(this).pref = 1-getPref(this));
		
		SipdroidReceiver.engine(this).register();
		System.out.println("ChangeAccount");
		finish();
	}
}
