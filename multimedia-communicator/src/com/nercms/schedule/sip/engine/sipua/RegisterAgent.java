/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
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

package com.nercms.schedule.sip.engine.sipua;

import java.util.Vector;

import com.nercms.schedule.misc.GD;
import com.nercms.schedule.sip.engine.sipua.ui.SipdroidReceiver;
import com.nercms.schedule.sip.engine.sipua.ui.Sipdroid;
import com.nercms.schedule.sip.stack.sip.address.NameAddress;
import com.nercms.schedule.sip.stack.sip.authentication.DigestAuthentication;
import com.nercms.schedule.sip.stack.sip.dialog.Dialog;
import com.nercms.schedule.sip.stack.sip.dialog.NotifierDialog;
import com.nercms.schedule.sip.stack.sip.dialog.SubscriberDialog;
import com.nercms.schedule.sip.stack.sip.dialog.SubscriberDialogListener;
import com.nercms.schedule.sip.stack.sip.header.AcceptHeader;
import com.nercms.schedule.sip.stack.sip.header.AuthorizationHeader;
import com.nercms.schedule.sip.stack.sip.header.ContactHeader;
import com.nercms.schedule.sip.stack.sip.header.ExpiresHeader;
import com.nercms.schedule.sip.stack.sip.header.Header;
import com.nercms.schedule.sip.stack.sip.header.ProxyAuthenticateHeader;
import com.nercms.schedule.sip.stack.sip.header.ProxyAuthorizationHeader;
import com.nercms.schedule.sip.stack.sip.header.StatusLine;
import com.nercms.schedule.sip.stack.sip.header.ViaHeader;
import com.nercms.schedule.sip.stack.sip.header.WwwAuthenticateHeader;
import com.nercms.schedule.sip.stack.sip.message.Message;
import com.nercms.schedule.sip.stack.sip.message.MessageFactory;
import com.nercms.schedule.sip.stack.sip.message.SipMethods;
import com.nercms.schedule.sip.stack.sip.provider.SipProvider;
import com.nercms.schedule.sip.stack.sip.provider.SipStack;
import com.nercms.schedule.sip.stack.sip.provider.TransactionIdentifier;
import com.nercms.schedule.sip.stack.sip.transaction.TransactionClient;
import com.nercms.schedule.sip.stack.sip.transaction.TransactionClientListener;
import com.nercms.schedule.sip.stack.tools.Log;
import com.nercms.schedule.sip.stack.tools.LogLevel;
import com.nercms.schedule.sip.stack.tools.Parser;

import com.nercms.schedule.sip.stack.sip.dialog.NotifierDialog;
import com.nercms.schedule.sip.stack.sip.dialog.NotifierDialogListener;

import android.preference.PreferenceManager;


/**
 * Register User Agent. It registers (one time or periodically) a contact
 * address with a registrar server.
 */
public class RegisterAgent implements TransactionClientListener, SubscriberDialogListener
{
	/** Max number of registration attempts. */
	static final int MAX_ATTEMPTS = 3;
	
	/* States for the RegisterAgent Module */
	public static final int UNDEFINED = 0;
	public static final int UNREGISTERED = 1;
	public static final int REGISTERING = 2;
	public static final int REGISTERED = 3;
	public static final int DEREGISTERING = 4;
	
	/** RegisterAgent listener */
	RegisterAgentListener listener;

	/** SipProvider */
	SipProvider sip_provider;

	/** User's URI with the fully qualified domain name of the registrar server. */
	NameAddress target;

	/** User name. */
	String username;

	/** User realm. */
	String realm;

	/** User's passwd. */
	String passwd;

	/** Q value for this registration (added by mandrajg)*/
	String qvalue;
	
	/** IMS Communication Service Identifier for this registration (currently only one supported)(added by mandrajg) */
	String icsi;	
	
	Boolean pub;
	
	/** Nonce for the next authentication. */
	String next_nonce;

	/** Qop for the next authentication. */
	String qop;

	/** User's contact address. */
	NameAddress contact;

	/** Expiration time. */
	int expire_time;

	/** Whether keep on registering. */
	boolean loop;

	/** Event logger. */
	Log log;

	/** Number of registration attempts. */
	int attempts,subattempts;

	/** Current State of the registrar component */
	int CurrentState;

	UserAgentProfile user_profile;

	SubscriberDialog sd;
	boolean alreadySubscribed = false;
	Message currentSubscribeMessage;
	public final int SUBSCRIPTION_EXPIRES = 184000;
	
	/**
	 * Creates a new RegisterAgent with authentication credentials (i.e.
	 * username, realm, and passwd).
	 */
	public RegisterAgent(SipProvider sip_provider, String target_url,
			String contact_url, String username, String realm, String passwd,
			RegisterAgentListener listener,UserAgentProfile user_profile,
			String qvalue, String icsi, Boolean pub) {									// modified by mandrajg
		
		init(sip_provider, target_url, contact_url, listener);
		
		//android.util.Log.v("UA", "RegisterAgent " + username + ", " + realm + ", " + passwd + ", " + user_profile);
		
		// authentication specific parameters
		this.username = username;
		this.realm = realm;
		this.passwd = passwd;
		this.user_profile = user_profile;
		
		// IMS specifics (added by mandrajg)
		this.qvalue = qvalue;
		this.icsi = icsi;
		
		this.pub = pub;
	}

	public void halt() {
		stopMWI();
		this.listener = null;
	}
	
	/** Inits the RegisterAgent. */
	private void init(SipProvider sip_provider, String target_url,
			String contact_url, RegisterAgentListener listener)
	{
		//android.util.Log.v("RTSP", "target_url " + target_url);//fym
		this.listener = listener;
		this.sip_provider = sip_provider;
		this.log = sip_provider.getLog();
		this.target = new NameAddress(target_url);
		this.contact = new NameAddress(contact_url);
		this.expire_time = SipStack.default_expires;//设置注册信令有效期
		
		// authentication
		this.username = null;
		this.realm = null;
		this.passwd = null;
		this.next_nonce = null;
		this.qop = null;
		this.attempts = 0;
	}

	/** Whether it is periodically registering. */
	public boolean isRegistered() {
		return (CurrentState == REGISTERED || CurrentState == REGISTERING);
	}
	
	/** Registers with the registrar server. */
	public boolean register()
	{
		return register(expire_time);
	}

	TransactionClient t;
	
	/** Registers with the registrar server for <i>expire_time</i> seconds. */
	public boolean register(int expire_time)
	{
		//android.util.Log.v("register", username + " register(int expire_time) " + Integer.toString(expire_time));//fym
		
		android.util.Log.i("register", "begin register current state" + CurrentState);
		
		attempts = 0;
		if (expire_time > 0)
		{
			//android.util.Log.v("UA", "register 2");//fym
			//Update this to be the default registration duration for next
			//instances as well.
			
			if (CurrentState == DEREGISTERING)
			{
				if (t != null) t.terminate();
				android.util.Log.v("register", "onTransTimeout 1");
				onTransTimeout(t);
			}
			if (CurrentState != UNREGISTERED && CurrentState != REGISTERED && CurrentState != UNDEFINED)
			{
				return false;
			}
			this.expire_time = expire_time;
			CurrentState = REGISTERING;
		}
		else
		{
			//android.util.Log.i("register", "shutdown 1.13321 " + expire_time);
			if (CurrentState == REGISTERING)
			{
				if (t != null) t.terminate();
				android.util.Log.v("register", "onTransTimeout 2");
				onTransTimeout(t);
			}
			//android.util.Log.i("register", "shutdown 1.13322 " + expire_time);
			if (CurrentState != REGISTERED && CurrentState != UNDEFINED)
			{
				//This is an error condition we must exit, we should not de-register if
				//we have not registered at all
				return false;
			}
			//android.util.Log.i("Temp", "shutdown 1.13323 " + expire_time);
			//this is the case for de-registration
			expire_time = 0;
			CurrentState = DEREGISTERING;
		}
		
		//android.util.Log.v("register", "register 3 " + target + ", " + user_profile.contact_url);//fym
		
		//android.util.Log.i("Temp", "shutdown 1.13325 " + expire_time);
		//Create message re (modified by mandrajg)
		Message req = MessageFactory.createRegisterRequest(sip_provider,
				target, target, new NameAddress(user_profile.contact_url), qvalue, icsi);
		
		//android.util.Log.i("Temp", "shutdown 1.13326 " + expire_time);
		//android.util.Log.v("register", "target " + target.toString());//fym
		
		req.setExpiresHeader(new ExpiresHeader(String.valueOf(expire_time)));
		
		//android.util.Log.i("register", "shutdown 1.13327 " + expire_time);
		
		//create and fill the authentication params this is done when
		//the UA has been challenged by the registrar or intermediate UA
		if (next_nonce != null) 
		{
			AuthorizationHeader ah = new AuthorizationHeader("Digest");
			
			ah.addUsernameParam(username);
			ah.addRealmParam(realm);
			ah.addNonceParam(next_nonce);
			ah.addUriParam(req.getRequestLine().getAddress().toString());
			ah.addQopParam(qop);
			String response = (new DigestAuthentication(SipMethods.REGISTER,
					ah, null, passwd)).getResponse();
			ah.addResponseParam(response);
			req.setAuthorizationHeader(ah);
		}
		
		//android.util.Log.i("Temp", "shutdown 1.13328 " + expire_time);
		
		if (expire_time > 0)
		{
			android.util.Log.v("SIP", "Registering contact " + contact + " (it expires in " + expire_time + " secs)");
		}
		else
		{
			//android.util.Log.i("register", "shutdown 1.13329 " + expire_time);
			//android.util.Log.v("register", "Unregistering contact " + contact);
		}
		
		//android.util.Log.v("register", "register 4");//fym
		
		android.util.Log.v("register", username + " register " + req.toString());
		
		//if(true == username.contains("T")) return true;
		
		t = new TransactionClient(sip_provider, req, this, 30000);
		t.request();
		
		//android.util.Log.i("", "shutdown 1.133210 " + expire_time);
		
		//android.util.Log.i("register","after register currentstate" + CurrentState);
		//android.util.Log.i("register","" + req);
		
		//android.util.Log.v("UA", "register 6");//fym
		
		return true;
	}

	/** Unregister with the registrar server */
	public boolean unregister()
	{
		android.util.Log.v("SIP", "unregister()");
		
		//android.util.Log.i("Temp", "shutdown 1.1331");
		stopMWI();
		//android.util.Log.i("Temp", "shutdown 1.1332");
		return register(0);
	}

	public void stopMWI()
	{
		if (sd != null) {
			synchronized (sd) {
				sd.notify();
			}
		}
		sd = null;
		if (listener != null) listener.onMWIUpdate(this, false, 0, null);
	}

	Message getSubscribeMessage(boolean current)
	{
		String empty = null;
		Message req;

		// Need to restart subscriber dialogue state engine
		if (sd != null) {
			synchronized (sd) {
				sd.notify();
			}
		}
		sd = new SubscriberDialog(sip_provider, "message-summary", "", this);
		/*sip_provider.removeSipProviderListener(new TransactionIdentifier(
				SipMethods.NOTIFY));
		sip_provider.addSipProviderListener(new TransactionIdentifier(
				SipMethods.NOTIFY), sd);*/
		if (current) {
			req = currentSubscribeMessage;
			req.setCSeqHeader(req.getCSeqHeader().incSequenceNumber());
		}
		else
		{
			android.util.Log.v("UserAgent", user_profile.contact_url);//fym
			
			req = MessageFactory.createSubscribeRequest(sip_provider,
				target.getAddress(), target, target,
				new NameAddress(user_profile.contact_url), sd.getEvent(),
				sd.getId(), empty, empty);
		}
		req.setExpiresHeader(new ExpiresHeader(SUBSCRIPTION_EXPIRES));
		req.setHeader(new AcceptHeader("application/simple-message-summary"));
		currentSubscribeMessage = req;
		return req;
	}
		

	public void startMWI()
	{
		if (alreadySubscribed)
			return;
		Message req = getSubscribeMessage(false);
		if(!SP.get(SipdroidReceiver.mContext, SP.PREF_MWI_ENABLED, SP.DEFAULT_MWI_ENABLED))
			return;
		if (sd != null) sd.subscribe(req);
	}

	void delayStartMWI()
	{
		if (subattempts < MAX_ATTEMPTS){
			subattempts++;
			Thread t = new Thread(new Runnable() {
					public void run() {
						Object o = new Object();
						try {
							synchronized (o) {
								o.wait(10000);
							}
						} catch (Exception E) {
						}
						startMWI();
					}
				});
			t.start();
		}
	}

	// **************** Subscription callback functions *****************
	public void onDlgSubscriptionSuccess(SubscriberDialog dialog, int code,
			String reason, Message resp)
	{
		final int expires;
		/* Can get replays of the subscription notice, so ignore */
		if (alreadySubscribed) {
			return;
		}
		alreadySubscribed = true;
		if (resp.hasExpiresHeader()) {
			if (0 == (expires = resp.getExpiresHeader().getDeltaSeconds()))
				return;
		} else {
			expires  = SUBSCRIPTION_EXPIRES;
		}
		Thread t = new Thread(new Runnable() {
				public void run() {
					try {
						synchronized (sd) {
							sd.wait(expires*1000);
						}
						alreadySubscribed = false;
						subattempts = 0;
						startMWI();
					} catch(Exception E) {
					}
				}
			});
		t.start();
	}

	public void onDlgSubscriptionFailure(SubscriberDialog dialog, int code,
			String reason, Message resp)
	{
		Message req = getSubscribeMessage(true);
		if (handleAuthentication(code, resp, req) && subattempts < MAX_ATTEMPTS) {
			subattempts++;
			sd.subscribe(req);
		} else {
			delayStartMWI();
		}
	}

	public void onDlgSubscribeTimeout(SubscriberDialog dialog)
	{
		delayStartMWI();
	}

	public void onDlgSubscriptionTerminated(SubscriberDialog dialog)
	{
		alreadySubscribed = false;
		startMWI();
	}

	public void onDlgNotify(SubscriberDialog dialog, NameAddress target,
			NameAddress notifier, NameAddress contact, String state,
			String content_type, String body, Message msg)
	{
		if(!SP.get(SipdroidReceiver.mContext, SP.PREF_MWI_ENABLED, SP.DEFAULT_MWI_ENABLED))
			return;
		Parser p = new Parser(body);
		final char[] propertysep = { ':', '\r', '\n' };
		final char[] vmailsep = { '/' }; 
		final char[] vmboxsep = { '@', '\r', '\n' };
		String vmaccount = null;
		boolean voicemail = false;
		int nummsg = 0;
		while (p.hasMore()) {
			String property = p.getWord(propertysep);
			p.skipChar();
			p.skipWSP();
			String value = p.getWord(Parser.CRLF);
			if (property.equalsIgnoreCase("Messages-Waiting") && value.equalsIgnoreCase("yes")) {
				voicemail = true;
			} else if (property.equalsIgnoreCase("Voice-Message")) {
				Parser np = new Parser(value);
				String num = np.getWord(vmailsep);
				nummsg = Integer.parseInt(num);
			} else if (property.equalsIgnoreCase("Message-Account")) {
				Parser np = new Parser(value);
				// strip the @<pbx> because it may have nat problems
				vmaccount = np.getWord(vmboxsep);
			}
		}
		if (listener != null) listener.onMWIUpdate(this, voicemail, nummsg, vmaccount);
	}

	// **************** Transaction callback functions *****************

	/** Callback function called when client sends back a failure response. */

	/** Callback function called when client sends back a provisional response. */
	public void onTransProvisionalResponse(TransactionClient transaction,
			Message resp) { // do nothing..
	}

	/** Callback function called when client sends back a success response. */
	public void onTransSuccessResponse(TransactionClient transaction,
			Message resp) 
	{
		if (transaction.getTransactionMethod().equals(SipMethods.REGISTER)) {
			
			if (resp.hasAuthenticationInfoHeader()) 
			{
				next_nonce = resp.getAuthenticationInfoHeader()
						.getNextnonceParam();
			}
			
			StatusLine status = resp.getStatusLine();
			String result = status.getCode() + " " + status.getReason();

			int expires = 0;
			if (resp.hasExpiresHeader()) 
			{
				expires = resp.getExpiresHeader().getDeltaSeconds();
			} 
			else if (resp.hasContactHeader()) 
			{
				Vector<Header> contacts = resp.getContacts().getHeaders();
				for (int i = 0; i < contacts.size(); i++) {
					int exp_i = (new ContactHeader((Header) contacts
							.elementAt(i))).getExpires();
					if (exp_i > 0 && (expires == 0 || exp_i < expires))
						expires = exp_i;
				}
			}
			
			printLog("Registration success: " + result, LogLevel.HIGH);
			
			if (CurrentState == REGISTERING)
			{
				CurrentState = REGISTERED;
				if (listener != null)
				{
					listener.onUaRegistrationSuccess(this, target, contact, result);
					SipdroidReceiver.reRegister(expires);//expires-15秒后重新注册
				}
			}
			else
			{
				CurrentState = UNREGISTERED;
				if (listener != null)
				{
					listener.onUaRegistrationSuccess(this, target, contact, result);
				}
			}
		}
	}

	/** Callback function called when client sends back a failure response. */
	public void onTransFailureResponse(TransactionClient transaction,
			Message resp) {
		if (transaction.getTransactionMethod().equals(SipMethods.REGISTER)) {
			StatusLine status = resp.getStatusLine();
			int code = status.getCode();
			if (!processAuthenticationResponse(transaction, resp, code)) {
				String result = code + " " + status.getReason();
				
				//Since the transactions are atomic, we rollback to the 
				//previous state
				if (CurrentState == REGISTERING)
				{
					CurrentState = UNREGISTERED;
					if (listener != null)
					{
						listener.onUaRegistrationFailure(this, target, contact,
								result);
						//Receiver.reRegister(1000);
					}
				}
				else
				{
					CurrentState = UNREGISTERED;
					if (listener != null)
					{
						listener.onUaRegistrationSuccess(this, target, contact, result);
					}
				}
				
				printLog("Registration failure: " + result, LogLevel.HIGH);
			}
		}
	}
	
	private boolean generateRequestWithProxyAuthorizationheader(
			Message resp, Message req){
		if(resp.hasProxyAuthenticateHeader()
				&& resp.getProxyAuthenticateHeader().getRealmParam()
				.length() > 0){
			user_profile.realm = realm = resp.getProxyAuthenticateHeader().getRealmParam();
			ProxyAuthenticateHeader pah = resp.getProxyAuthenticateHeader();
			String qop_options = pah.getQopOptionsParam();
			
			printLog("DEBUG: qop-options: " + qop_options, LogLevel.MEDIUM);
			
			qop = (qop_options != null) ? "auth" : null;
			
			ProxyAuthorizationHeader ah = (new DigestAuthentication(
							req.getTransactionMethod(), req.getRequestLine().getAddress()
							.toString(), pah, qop, null, username, passwd))
					.getProxyAuthorizationHeader();
			req.setProxyAuthorizationHeader(ah);
			
			return true;
		}
		return false;
	}
	
	private boolean generateRequestWithWwwAuthorizationheader(
			Message resp, Message req){
		if(resp.hasWwwAuthenticateHeader()
				&& resp.getWwwAuthenticateHeader().getRealmParam()
				.length() > 0){		
			user_profile.realm = realm = resp.getWwwAuthenticateHeader().getRealmParam();
			WwwAuthenticateHeader wah = resp.getWwwAuthenticateHeader();
			String qop_options = wah.getQopOptionsParam();
			
			printLog("DEBUG: qop-options: " + qop_options, LogLevel.MEDIUM);
			
			qop = (qop_options != null) ? "auth" : null;
			
			AuthorizationHeader ah = (new DigestAuthentication(
							req.getTransactionMethod(), req.getRequestLine().getAddress()
							.toString(), wah, qop, null, username, passwd))
					.getAuthorizationHeader();
			req.setAuthorizationHeader(ah);
			return true;
		}
		return false;
	}

	private boolean handleAuthentication(int respCode, Message resp,
					     Message req) {
		switch (respCode) {
		case 407:
			return generateRequestWithProxyAuthorizationheader(resp, req);
		case 401:
			return generateRequestWithWwwAuthorizationheader(resp, req);
		}
		return false;
	}
		
	
	private boolean processAuthenticationResponse(TransactionClient transaction,
			Message resp, int respCode){
		if (attempts < MAX_ATTEMPTS){
			attempts++;
			Message req = transaction.getRequestMessage();
			req.setCSeqHeader(req.getCSeqHeader().incSequenceNumber());
			ViaHeader vh=req.getViaHeader();
			String newbranch = SipProvider.pickBranch();
			vh.setBranch(newbranch);	
			req.removeViaHeader();
			req.addViaHeader(vh);

			if (handleAuthentication(respCode, resp, req)) {
				t = new TransactionClient(sip_provider, req, this, 30000);
			
				t.request();
				return true;
			}
		}
		return false;
	}
	
	/** Callback function called when client expires timeout. */
	public void onTransTimeout(TransactionClient transaction)
	{
		if (transaction == null) return;
		if (transaction.getTransactionMethod().equals(SipMethods.REGISTER))
		{
			printLog("Registration failure: No response from server.", LogLevel.HIGH);
			
			//Since the transactions are atomic, we rollback to the 
			//previous state
			
			if (CurrentState == REGISTERING)
			{
				CurrentState = UNDEFINED;
				
				if (listener != null)
				{
					listener.onUaRegistrationFailure(this, target, contact, "Timeout");
					//Receiver.reRegister(1000);
				}
			}
			else
			{
				if (pub && android.provider.Settings.System.getInt(
					      SipdroidReceiver.mContext.getContentResolver(), 
					      android.provider.Settings.System.AIRPLANE_MODE_ON, 0) == 0)
				{
					CurrentState = UNDEFINED;
					if (listener != null)
					{
						listener.onUaRegistrationFailure(this, target, contact, "Timeout");
						//Receiver.reRegister(1000);
					}
				}
				else
				{
					CurrentState = UNREGISTERED;
					if (listener != null)
					{
						listener.onUaRegistrationSuccess(this, target, contact, "Timeout");
					}
				}
			}
		}
	}

	// ****************************** Logs *****************************

	/** Adds a new string to the default Log */
	void printLog(String str, int level) {		
		/*if (Sipdroid.release) return;
		if (log != null)
			log.println("RegisterAgent: " + str, level + SipStack.LOG_LEVEL_UA);
		if (level <= LogLevel.HIGH)
			System.out.println("RegisterAgent: " + str);*/
		
		android.util.Log.v("RegisterAgent", str);//fym
	}

	/** Adds the Exception message to the default Log */
	void printException(Exception e, int level) {
		/*if (Sipdroid.release) return;
		if (log != null)
			log.printException(e, level + SipStack.LOG_LEVEL_UA);*/
		
		android.util.Log.v("RegisterAgent", "Exception: " + e.toString());//fym
	}
	
	//fym
	public void send_message(String ua_id, String message_content)
	{
		//fym String username = SipPreference.get_parameter(Receiver.mContext, SipPreference.PREF_USERNAME, "111111");
		String from_username = Long.toString(GD.get_unique_id(SipdroidReceiver.mContext));
		from_username += (true == sip_provider.is_over_tcp()) ? "T" : "U";
		
		String server = GD.DEFAULT_SCHEDULE_SERVER;//SP.get(SipdroidReceiver.mContext, SP.PREF_SCHEDULE_SERVER, "");
		
		Message msg = MessageFactory.createMessageRequest(sip_provider,
							new NameAddress(ua_id + "@" + ((false == GD.OVER_VPDN) ? server : GD.SIP_SERVER_LAN_IP_IN_VPN)),
							new NameAddress(from_username + "@" + server),
							"", "text/plain", message_content);
		
		TransactionClient message_transaction = new TransactionClient(sip_provider, msg, this);
		message_transaction.request();
	}
}
