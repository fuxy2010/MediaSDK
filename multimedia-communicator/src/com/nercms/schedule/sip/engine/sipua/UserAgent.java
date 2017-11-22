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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nercms.schedule.sip.engine.sipua.ui.SipdroidReceiver;
import com.nercms.schedule.sip.stack.net.IpAddress;
import com.nercms.schedule.sip.stack.sdp.AttributeField;
import com.nercms.schedule.sip.stack.sdp.ConnectionField;
import com.nercms.schedule.sip.stack.sdp.MediaDescriptor;
import com.nercms.schedule.sip.stack.sdp.MediaField;
import com.nercms.schedule.sip.stack.sdp.SessionDescriptor;
import com.nercms.schedule.sip.stack.sdp.TimeField;
import com.nercms.schedule.sip.stack.sip.address.NameAddress;
import com.nercms.schedule.sip.stack.sip.call.Call;
import com.nercms.schedule.sip.stack.sip.call.CallListenerAdapter;
import com.nercms.schedule.sip.stack.sip.call.ExtendedCall;
import com.nercms.schedule.sip.stack.sip.call.SdpTools;
import com.nercms.schedule.sip.stack.sip.header.StatusLine;
import com.nercms.schedule.sip.stack.sip.message.Message;
import com.nercms.schedule.sip.stack.sip.message.MessageFactory;
import com.nercms.schedule.sip.stack.sip.message.SipResponses;
import com.nercms.schedule.sip.stack.sip.provider.SipProvider;
import com.nercms.schedule.sip.stack.sip.provider.SipProviderListener;
import com.nercms.schedule.sip.stack.sip.transaction.TransactionServer;
import com.nercms.schedule.sip.stack.tools.Log;
import com.nercms.schedule.sip.stack.tools.LogLevel;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;
import com.nercms.schedule.misc.Participant;
import com.nercms.schedule.ui.MessageHandlerManager;

/**
 * Simple SIP user agent (UA). It includes audio/video applications.
 * <p>
 * It can use external audio/video tools as media applications. Currently only
 * RAT (Robust Audio Tool) and VIC are supported as external applications.
 */
public class UserAgent extends CallListenerAdapter implements SipProviderListener{//fym public class UserAgent extends CallListenerAdapter {
	/** Event logger. */
	Log log;

	/** UserAgentProfile */
	public UserAgentProfile user_profile;

	/** SipProvider */
	protected SipProvider sip_provider;

	/** Call */
	// Call call;
	protected ExtendedCall call;

	/** Call transfer */
	protected ExtendedCall call_transfer;
	
	//fym
	/*Message Transaction*/
	protected TransactionServer message_trans;

	/** Audio application */
	//fym public MediaLauncher audio_app = null;

	/** Local sdp */
	protected String local_session = null;
	
	public static final int UA_STATE_IDLE = 0;
	public static final int UA_STATE_INCOMING_CALL = 1;
	public static final int UA_STATE_OUTGOING_CALL = 2;
	public static final int UA_STATE_INCALL = 3;
	public static final int UA_STATE_HOLD = 4;

	int call_state = UA_STATE_IDLE;
	String remote_media_address;
	int remote_video_port,local_video_port;

	// *************************** Basic methods ***************************

	/** Changes the call state */
	protected synchronized void changeStatus(int state,String caller) {
		call_state = state;
		SipdroidReceiver.onState(state, caller);
	}
	
	protected void changeStatus(int state) {
		changeStatus(state, null);
	}

	/** Checks the call state */
	protected boolean statusIs(int state) {
		return (call_state == state);
	}

	/**
	 * Sets the automatic answer time (default is -1 that means no auto accept
	 * mode)
	 */
	public void setAcceptTime(int accept_time) {
		user_profile.accept_time = accept_time;
	}

	/**
	 * Sets the automatic hangup time (default is 0, that corresponds to manual
	 * hangup mode)
	 */
	public void setHangupTime(int time) {
		user_profile.hangup_time = time;
	}

	/** Sets the redirection url (default is null, that is no redircetion) */
	public void setRedirection(String url) {
		user_profile.redirect_to = url;
	}

	/** Sets the no offer mode for the invite (default is false) */
	public void setNoOfferMode(boolean nooffer) {
		user_profile.no_offer = nooffer;
	}

	/** Enables audio */
	public void setAudio(boolean enable) {
		user_profile.audio = enable;
	}

	/** Sets the receive only mode */
	public void setReceiveOnlyMode(boolean r_only) {
		user_profile.recv_only = r_only;
	}

	/** Sets the send only mode */
	public void setSendOnlyMode(boolean s_only) {
		user_profile.send_only = s_only;
	}

	/** Sets the send tone mode */
	public void setSendToneMode(boolean s_tone) {
		user_profile.send_tone = s_tone;
	}

	/** Sets the send file */
	
	public void setSendFile(String file_name) {
		user_profile.send_file = file_name;
	}

	/** Sets the recv file */
	
	public void setRecvFile(String file_name) {
		user_profile.recv_file = file_name;
	}
	
	/** Gets the local SDP */
	public String getSessionDescriptor() {
		return local_session;
	}

	//change start (multi codecs)
	/** Inits the local SDP (no media spec) */
	/*fym public void initSessionDescriptor(Codecs.Map c) {
		SessionDescriptor sdp = new SessionDescriptor(
				user_profile.from_url,
				sip_provider.getViaAddress());
		
		local_session = sdp.toString();
		
		//We will have at least one media line, and it will be 
		//audio
		if (user_profile.audio || !user_profile.video)
		{
//			addMediaDescriptor("audio", user_profile.audio_port, c, user_profile.audio_sample_rate);
			addMediaDescriptor("audio", user_profile.audio_port, c);
		}
		
		if (user_profile.video)
		{
			addMediaDescriptor("video", user_profile.video_port,
					user_profile.video_avp, "h263-1998", 90000);
		}
	}*/
	public void initSessionDescriptor()
	{
		SessionDescriptor sdp = new SessionDescriptor(
				user_profile.from_url,
				sip_provider.getViaAddress());
		
		local_session = sdp.toString();
		
		addMediaDescriptor("audio", 10000);
		addMediaDescriptor("video", 20000, 103, "h264", 90000);
		
		//android.util.Log.v("UA", "initSessionDescriptor: " + local_session);
	}
	//change end
	
	/** Adds a single media to the SDP */
	private void addMediaDescriptor(String media, int port, int avp,
					String codec, int rate) {
		SessionDescriptor sdp = new SessionDescriptor(local_session);
		
		String attr_param = String.valueOf(avp);
		
		if (codec != null)
		{
			attr_param += " " + codec + "/" + rate;
		}
		sdp.addMedia(new MediaField(media, port, 0, "RTP/AVP", 
				String.valueOf(avp)), 
				new AttributeField("rtpmap", attr_param));
		
		local_session = sdp.toString();
	}
	
	/** Adds a set of media to the SDP */
//	private void addMediaDescriptor(String media, int port, Codecs.Map c,int rate) {
	/*fym private void addMediaDescriptor(String media, int port, Codecs.Map c) {
		SessionDescriptor sdp = new SessionDescriptor(local_session);
	
		Vector<String> avpvec = new Vector<String>();
		Vector<AttributeField> afvec = new Vector<AttributeField>();
		if (c == null) {
			// offer all known codecs
			for (int i : Codecs.getCodecs()) {
				Codec codec = Codecs.get(i);
				if (i == 0) codec.init();
				avpvec.add(String.valueOf(i));
				if (codec.number() == 9)
					afvec.add(new AttributeField("rtpmap", String.format("%d %s/%d", i, codec.userName(), 8000))); // kludge for G722. See RFC3551.
				else
					afvec.add(new AttributeField("rtpmap", String.format("%d %s/%d", i, codec.userName(), codec.samp_rate())));
			}
		} else {
			c.codec.init();
			avpvec.add(String.valueOf(c.number));
			if (c.codec.number() == 9)
				afvec.add(new AttributeField("rtpmap", String.format("%d %s/%d", c.number, c.codec.userName(), 8000))); // kludge for G722. See RFC3551.
			else
				afvec.add(new AttributeField("rtpmap", String.format("%d %s/%d", c.number, c.codec.userName(), c.codec.samp_rate())));
		}
		if (user_profile.dtmf_avp != 0){
			avpvec.add(String.valueOf(user_profile.dtmf_avp));
			afvec.add(new AttributeField("rtpmap", String.format("%d telephone-event/%d", user_profile.dtmf_avp, user_profile.audio_sample_rate)));
			afvec.add(new AttributeField("fmtp", String.format("%d 0-15", user_profile.dtmf_avp)));
		}
				
		//String attr_param = String.valueOf(avp);
		
		sdp.addMedia(new MediaField(media, port, 0, "RTP/AVP", avpvec), afvec);
		
		local_session = sdp.toString();
	}*/
	private void addMediaDescriptor(String media, int port)
	{
		SessionDescriptor sdp = new SessionDescriptor(local_session);
	
		Vector<String> avpvec = new Vector<String>();
		Vector<AttributeField> afvec = new Vector<AttributeField>();
		
		avpvec.add("8");
		afvec.add(new AttributeField("rtpmap", "8 PCMA/8000"));
		
		avpvec.add("0");
		afvec.add(new AttributeField("rtpmap", "0 PCMU/8000"));
		
		/*if (user_profile.dtmf_avp != 0){
			avpvec.add(String.valueOf(user_profile.dtmf_avp));
			afvec.add(new AttributeField("rtpmap", String.format("%d telephone-event/%d", user_profile.dtmf_avp, user_profile.audio_sample_rate)));
			afvec.add(new AttributeField("fmtp", String.format("%d 0-15", user_profile.dtmf_avp)));
		}*/
				
		//String attr_param = String.valueOf(avp);
		
		sdp.addMedia(new MediaField(media, port, 0, "RTP/AVP", avpvec), afvec);
		
		local_session = sdp.toString();
	}

	// *************************** Public Methods **************************

	/** Costructs a UA with a default media port */
	public UserAgent(SipProvider sip_provider, UserAgentProfile user_profile)
	{
		this.sip_provider = sip_provider;
		log = sip_provider.getLog();
		this.user_profile = user_profile;
		realm = user_profile.realm;
		
		// if no contact_url and/or from_url has been set, create it now
		user_profile.initContactAddress(sip_provider);
		
		this.sip_provider.addSipProviderListener(this);//fym 实现对MESSAGE消息的监听
	}

	String realm;
	
	/** Makes a new call (acting as UAC). */
	public boolean call(String target_url, boolean send_anonymous)
	{
		
		if (SipdroidReceiver.call_state != UA_STATE_IDLE)
		{
			//We can initiate or terminate a call only when
			//we are in an idle state
			printLog("Call attempted in state" + this.getSessionDescriptor() + " : Failing Request", LogLevel.HIGH);
			return false;
		}
		android.util.Log.v("UA", "hang up 1");
		hangup(); // modified
		changeStatus(UA_STATE_OUTGOING_CALL,target_url);
		
		String from_url;
		
		if (!send_anonymous)
		{
			from_url = user_profile.from_url;
		}
		else
		{
			from_url = "sip:anonymous@anonymous.com";
		}

		//change start multi codecs
		createOffer();
		//change end
		call = new ExtendedCall(sip_provider, from_url,
				user_profile.contact_url, user_profile.username,
				user_profile.realm, user_profile.passwd, this);
		
		// in case of incomplete url (e.g. only 'user' is present), try to
		// complete it
		if (target_url.indexOf("@") < 0)
		{
			if (user_profile.realm.equals(""/*fym SipPreference.DEFAULT_SERVER*/))
				target_url = "&" + target_url;
			
			target_url = target_url + "@" + realm; // modified
		}
		
		// MMTel addition to define MMTel ICSI to be included in INVITE (added by mandrajg)
		String icsi = null;	
		if (user_profile.mmtel == true){
			icsi = "\"urn%3Aurn-7%3A3gpp-service.ims.icsi.mmtel\"";
		}		
		
		target_url = sip_provider.completeNameAddress(target_url).toString();
		
		if (user_profile.no_offer)
		{
			call.call(target_url);
		}
		else
		{
			call.call(target_url, local_session, icsi);		// modified by mandrajg
		}
		
		return true;
	}

	public void info(char c, int duration)
	{
		/*boolean use2833 = audio_app != null && audio_app.sendDTMF(c); // send out-band DTMF (rfc2833) if supported

		if (!use2833 && call != null)
			call.info(c, duration);*/
	}
	
	/** Waits for an incoming call (acting as UAS). */
	public boolean listen()
	{
		android.util.Log.v("SIP", "listen");
		
		if (SipdroidReceiver.call_state != UA_STATE_IDLE)
		{
			//We can listen for a call only when
			//we are in an idle state
			printLog("Call listening mode initiated in " + this.getSessionDescriptor() + " : Failing Request", LogLevel.HIGH);
			return false;
		}
		
		android.util.Log.v("UA", "hang up 2");
		hangup();
		
		call = new ExtendedCall(sip_provider, user_profile.from_url,
				user_profile.contact_url, user_profile.username,
				user_profile.realm, user_profile.passwd, this);
		
		android.util.Log.v("UA", "listen 4");
		
		call.listen();
		
		return true;
	}

	/** Closes an ongoing, incoming, or pending call */
	public void hangup()
	{
		android.util.Log.v("SIP", "====== hang up");
		
		closeMediaApplication();
		
		if (call != null)
		{
			call.hangup();
		}
		
		changeStatus(UA_STATE_IDLE);
	}

	/** Accepts an incoming call */
	public boolean accept() 
	{
		android.util.Log.v("SIP", "====== accept");
		
		if (call == null)
		{
			return false;
		}
		
		printLog("ACCEPT");
		changeStatus(UA_STATE_INCALL); // modified

		call.accept(local_session);
		
		return true;
	}

	/** Redirects an incoming call */
	public void redirect(String redirection) 
	{
		android.util.Log.v("SIP", "redirect");
		
		if (call != null)
		{
			call.redirect(redirection);
		}
	}

	/** Launches the Media Application (currently, the RAT audio tool) */
	protected void launchMediaApplication()
	{
		// exit if the Media Application is already running
		/*fym if (audio_app != null) {
			printLog("DEBUG: media application is already running",
					LogLevel.HIGH);
			return;
		}
		Codecs.Map c;
		// parse local sdp
		SessionDescriptor local_sdp = new SessionDescriptor(call
				.getLocalSessionDescriptor());
		int local_audio_port = 0;
		local_video_port = 0;
		int dtmf_pt = 0;
		c = Codecs.getCodec(local_sdp);
		if (c == null) {
			Receiver.call_end_reason = R.string.card_title_ended_no_codec;
			hangup();
			return;
		}
		MediaDescriptor m = local_sdp.getMediaDescriptor("video");
		if ( m != null)
			local_video_port = m.getMedia().getPort();
		m = local_sdp.getMediaDescriptor("audio");
		if (m != null) {
			local_audio_port = m.getMedia().getPort();
			if (m.getMedia().getFormatList().contains(String.valueOf(user_profile.dtmf_avp)))
				dtmf_pt = user_profile.dtmf_avp;
		}
		// parse remote sdp
		SessionDescriptor remote_sdp = new SessionDescriptor(call
				.getRemoteSessionDescriptor());
		remote_media_address = (new Parser(remote_sdp.getConnection()
				.toString())).skipString().skipString().getString();
		int remote_audio_port = 0;
		remote_video_port = 0;
		for (Enumeration<MediaDescriptor> e = remote_sdp.getMediaDescriptors()
				.elements(); e.hasMoreElements();) {
			MediaField media = e.nextElement().getMedia();
			if (media.getMedia().equals("audio"))
				remote_audio_port = media.getPort();
			if (media.getMedia().equals("video"))
				remote_video_port = media.getPort();
		}

		// select the media direction (send_only, recv_ony, fullduplex)
		int dir = 0;
		if (user_profile.recv_only)
			dir = -1;
		else if (user_profile.send_only)
			dir = 1;

		if (user_profile.audio && local_audio_port != 0
				&& remote_audio_port != 0) { // create an audio_app and start
												// it

			if (audio_app == null) { // for testing..
				String audio_in = null;
				if (user_profile.send_tone) {
					audio_in = JAudioLauncher.TONE;
				} else if (user_profile.send_file != null) {
					audio_in = user_profile.send_file;
				}
				String audio_out = null;
				if (user_profile.recv_file != null) {
					audio_out = user_profile.recv_file;
				}

				audio_app = new JAudioLauncher(local_audio_port,
						remote_media_address, remote_audio_port, dir, audio_in,
						audio_out, c.codec.samp_rate(),
						user_profile.audio_sample_size,
						c.codec.frame_size(), log, c, dtmf_pt);
			}
			audio_app.startMedia();
		}*/
	}

	/** Close the Media Application */
	protected void closeMediaApplication() {
		/*fym if (audio_app != null) {
			audio_app.stopMedia();
			audio_app = null;
		}*/
	}
	
	public boolean muteMediaApplication() {
		return true;//fym
		/*fym if (audio_app != null)
			return audio_app.muteMedia();
		return false;*/
	}

	public int speakerMediaApplication(int mode) {
		return 1;
		/*fym int old;
		
		if (audio_app != null)
			return audio_app.speakerMedia(mode);
		old = RtpStreamReceiver.speakermode;
		RtpStreamReceiver.speakermode = mode;
		return old;*/
	}

	public void bluetoothMediaApplication() {
		/*fym if (audio_app != null)
			audio_app.bluetoothMedia();*/
	}

	private void createOffer() {
		initSessionDescriptor();//fym initSessionDescriptor(null);
	}

	private void createAnswer(SessionDescriptor remote_sdp) {

		/*fym Codecs.Map c = Codecs.getCodec(remote_sdp);
		if (c == null)
			throw new RuntimeException("Failed to get CODEC: AVAILABLE : " + remote_sdp);
		initSessionDescriptor(c);
		sessionProduct(remote_sdp);*/
		
		initSessionDescriptor();//fym
		sessionProduct(remote_sdp);//fym
	}

	private void sessionProduct(SessionDescriptor remote_sdp) {
		SessionDescriptor local_sdp = new SessionDescriptor(local_session);
		SessionDescriptor new_sdp = new SessionDescriptor(local_sdp
				.getOrigin(), local_sdp.getSessionName(), local_sdp
				.getConnection(), local_sdp.getTime());
		new_sdp.addMediaDescriptors(local_sdp.getMediaDescriptors());
		new_sdp = SdpTools.sdpMediaProduct(new_sdp, remote_sdp
				.getMediaDescriptors());
		//new_sdp = SdpTools.sdpAttirbuteSelection(new_sdp, "rtpmap"); ////change multi codecs
		local_session = new_sdp.toString();
		if (call!=null) call.setLocalSessionDescriptor(local_session);
	}

	// ********************** Call callback functions **********************

	/**
	 * Callback function called when arriving a new INVITE method (incoming
	 * call)
	 */
	public void onCallIncoming(Call call, NameAddress callee,
			NameAddress caller, String sdp, Message invite)
	{
		//android.util.Log.v("Debug", "onCallIncoming: " + sdp);//fym
		//android.util.Log.i("SIP", "====== incoming call " + caller.toString());
		
		printLog("onCallIncoming()", LogLevel.LOW);
		
		if (call != this.call)
		{
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		
		//android.util.Log.i("SIP", "====== incoming call 2");
		
		printLog("INCOMING", LogLevel.HIGH);
		int i = 0;
		
		for (UserAgent ua : SipdroidReceiver._unique_sipdroid_engine.uas)
		{
			if (ua == this) break;
			i++;
		}
		
		//android.util.Log.i("SIP", "====== incoming call 3");
		
		if (SipdroidReceiver.call_state != UA_STATE_IDLE || false)//fym !Receiver.isFast(i))
		{
			call.busy();
			android.util.Log.v("UA", "listen 5");
			listen();
			return;
		}
		
		//android.util.Log.i("SIP", "====== incoming call 5");
		
		if (SipdroidReceiver._unique_sipdroid_engine != null)
		{
			SipdroidReceiver._unique_sipdroid_engine.ua = this;
		}
		
		changeStatus(UA_STATE_INCOMING_CALL,caller.toString());
		
		//android.util.Log.i("SIP", "====== incoming call 6");

		if (sdp == null)
		{
			createOffer();
			
			//android.util.Log.i("SIP", "====== incoming call 7.1");
		}
		else
		{ 
			SessionDescriptor remote_sdp = new SessionDescriptor(sdp);
			try
			{
				createAnswer(remote_sdp);
			}
			catch (Exception e)
			{
				// only known exception is no codec
				SipdroidReceiver.call_end_reason = 0x7f06007f;//fym R.string.card_title_ended_no_codec;
				changeStatus(UA_STATE_IDLE);
				return;
			}
			
			//fym
			//从服务器发送的INVITE消息体中获取服务器音视频接收端口
			MediaDescriptor _audio = remote_sdp.getMediaDescriptor("audio");
			if(null != _audio)
			{
				GD.SERVER_AUDIO_RECV_PORT = _audio.getMedia().getPort();
			}
			
			MediaDescriptor _video = remote_sdp.getMediaDescriptor("video");
			if(null != _video)
			{
				GD.SERVER_VIDEO_RECV_PORT = _video.getMedia().getPort();
			}
			
			//android.util.Log.v("UA", sdp);
			android.util.Log.i("SIP", "ServerAudioRecvPort: " + GD.SERVER_AUDIO_RECV_PORT + ", ServerVideoRecvPort: " + GD.SERVER_VIDEO_RECV_PORT);
			
			//android.util.Log.i("SIP", "====== incoming call 7.2");
			
		}
		call.ring(local_session);		
		launchMediaApplication();
		
		android.util.Log.v("UA", "onCallIncoming()");
	}

	/**
	 * Callback function called when arriving a new Re-INVITE method
	 * (re-inviting/call modify)
	 */
	public void onCallModifying(Call call, String sdp, Message invite) 
	{
		printLog("onCallModifying()", LogLevel.LOW);
		if (call != this.call) 
		{
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("RE-INVITE/MODIFY", LogLevel.HIGH);

		// to be implemented.
		// currently it simply accepts the session changes (see method
		// onCallModifying() in CallListenerAdapter)
		super.onCallModifying(call, sdp, invite);
	}

	/**
	 * Callback function that may be overloaded (extended). Called when arriving
	 * a 180 Ringing or a 183 Session progress with SDP 
	 */
	public void onCallRinging(Call call, Message resp)
	{
		android.util.Log.v("SIP", "====== onCallRinging");
		
		printLog("onCallRinging()", LogLevel.LOW);
		if (call != this.call && call != call_transfer) 
		{
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		
		String remote_sdp = call.getRemoteSessionDescriptor();
		if (remote_sdp==null || remote_sdp.length()==0) {
			printLog("RINGING", LogLevel.HIGH);
			//fym RtpStreamReceiver.ringback(true);
		}
		else {
			printLog("RINGING(with SDP)", LogLevel.HIGH);
			if (! user_profile.no_offer) { 
				//fym RtpStreamReceiver.ringback(false);
				// Update the local SDP along with offer/answer 
				sessionProduct(new SessionDescriptor(remote_sdp));
				launchMediaApplication();
			}
		}
	}

	/** Callback function called when arriving a 2xx (call accepted) */
	public void onCallAccepted(Call call, String sdp, Message resp) 
	{
		android.util.Log.v("SIP", "====== onCallAccepted");
		
		printLog("onCallAccepted()", LogLevel.LOW);
		
		if (call != this.call && call != call_transfer) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		
		printLog("ACCEPTED/CALL", LogLevel.HIGH);
		
		if (!statusIs(UA_STATE_OUTGOING_CALL)) { // modified
			android.util.Log.v("UA", "hang up 3");
			hangup();
			return;
		}
		changeStatus(UA_STATE_INCALL);
		
		SessionDescriptor remote_sdp = new SessionDescriptor(sdp);
		if (user_profile.no_offer) {
			// answer with the local sdp
			createAnswer(remote_sdp);
			call.ackWithAnswer(local_session);
		} else {
			// Update the local SDP along with offer/answer 
			sessionProduct(remote_sdp);
		}
		launchMediaApplication();

		if (call == call_transfer) 
		{
			StatusLine status_line = resp.getStatusLine();
			int code = status_line.getCode();
			String reason = status_line.getReason();
			this.call.notify(code, reason);
		}
	}

	/** Callback function called when arriving an ACK method (call confirmed) */
	public void onCallConfirmed(Call call, String sdp, Message ack) 
	{
		android.util.Log.v("SIP", "====== onCallConfirmed");
		
		printLog("onCallConfirmed()", LogLevel.LOW);
	
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		
		printLog("CONFIRMED/CALL", LogLevel.HIGH);

//		changeStatus(UA_STATE_INCALL); modified
		
		if (user_profile.hangup_time > 0)
		{
			this.automaticHangup(user_profile.hangup_time);
		}
	}

	/** Callback function called when arriving a 2xx (re-invite/modify accepted) */
	public void onCallReInviteAccepted(Call call, String sdp, Message resp) {
		printLog("onCallReInviteAccepted()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("RE-INVITE-ACCEPTED/CALL", LogLevel.HIGH);
		if (statusIs(UA_STATE_HOLD))
			changeStatus(UA_STATE_INCALL);
		else
			changeStatus(UA_STATE_HOLD);
	}

	/** Callback function called when arriving a 4xx (re-invite/modify failure) */
	public void onCallReInviteRefused(Call call, String reason, Message resp) {
		printLog("onCallReInviteRefused()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("RE-INVITE-REFUSED (" + reason + ")/CALL", LogLevel.HIGH);
	}

	/** Callback function called when arriving a 4xx (call failure) */
	public void onCallRefused(Call call, String reason, Message resp) {
		printLog("onCallRefused()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("REFUSED (" + reason + ")", LogLevel.HIGH);
		if (reason.equalsIgnoreCase("not acceptable here")) {
			// bummer we have to string compare, this is sdp 488
			SipdroidReceiver.call_end_reason = 0x7f06007f;//fym R.string.card_title_ended_no_codec;
		}
		changeStatus(UA_STATE_IDLE);
		
		if (call == call_transfer) 
		{
			StatusLine status_line = resp.getStatusLine();
			int code = status_line.getCode();
			// String reason=status_line.getReason();
			this.call.notify(code, reason);
			call_transfer = null;
		}
	}

	/** Callback function called when arriving a 3xx (call redirection) */
	public void onCallRedirection(Call call, String reason,
			Vector<String> contact_list, Message resp) {
		printLog("onCallRedirection()", LogLevel.LOW);
		if (call != this.call) 
		{
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("REDIRECTION (" + reason + ")", LogLevel.HIGH);
		call.call(((String) contact_list.elementAt(0)));
	}

	/**
	 * Callback function that may be overloaded (extended). Called when arriving
	 * a CANCEL request
	 */
	public void onCallCanceling(Call call, Message cancel)
	{
		android.util.Log.v("SIP", "====== onCallCanceling");
		
		printLog("onCallCanceling()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("CANCEL", LogLevel.HIGH);
		changeStatus(UA_STATE_IDLE);
		
		//得到cancel消息后 通知接听界面销毁
		MessageHandlerManager.get_instance().handle_message(GID.MSG_RECV_CANCEL,GD.MEDIA_INSTANCE);
	}

	/** Callback function called when arriving a BYE request */
	public void onCallClosing(Call call, Message bye)
	{
		android.util.Log.v("SIP", "====== onCallClosing");
		
		printLog("onCallClosing()", LogLevel.LOW);
		if (call != this.call && call != call_transfer) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}

		if (call != call_transfer && call_transfer != null) {
			printLog("CLOSE PREVIOUS CALL", LogLevel.HIGH);
			this.call = call_transfer;
			call_transfer = null;
			return;
		}
		// else
		printLog("CLOSE", LogLevel.HIGH);
		closeMediaApplication();
		changeStatus(UA_STATE_IDLE);
		
		//得到bye消息后 通知接听界面销毁
		android.util.Log.v("Media", "hang up 1");
		MessageHandlerManager.get_instance().handle_message(GID.MSG_HANG_UP,GD.MEDIA_INSTANCE);
	}

	/**
	 * Callback function called when arriving a response after a BYE request
	 * (call closed)
	 */
	public void onCallClosed(Call call, Message resp) {
		printLog("onCallClosed()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("CLOSE/OK", LogLevel.HIGH);
		
		changeStatus(UA_STATE_IDLE);
	}

	/** Callback function called when the invite expires */
	public void onCallTimeout(Call call) {
		printLog("onCallTimeout()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("NOT FOUND/TIMEOUT", LogLevel.HIGH);
		changeStatus(UA_STATE_IDLE);
		if (call == call_transfer) {
			int code = 408;
			String reason = "Request Timeout";
			this.call.notify(code, reason);
			call_transfer = null;
		}
	}

	// ****************** ExtendedCall callback functions ******************

	/**
	 * Callback function called when arriving a new REFER method (transfer
	 * request)
	 */
	public void onCallTransfer(ExtendedCall call, NameAddress refer_to,
			NameAddress refered_by, Message refer) {
		printLog("onCallTransfer()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("Transfer to " + refer_to.toString(), LogLevel.HIGH);
		call.acceptTransfer();
		call_transfer = new ExtendedCall(sip_provider, user_profile.from_url,
				user_profile.contact_url, this);
		call_transfer.call(refer_to.toString(), local_session, null); 		// modified by mandrajg
	}

	/** Callback function called when a call transfer is accepted. */
	public void onCallTransferAccepted(ExtendedCall call, Message resp) {
		printLog("onCallTransferAccepted()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("Transfer accepted", LogLevel.HIGH);
	}

	/** Callback function called when a call transfer is refused. */
	public void onCallTransferRefused(ExtendedCall call, String reason,
			Message resp) {
		printLog("onCallTransferRefused()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("Transfer refused", LogLevel.HIGH);
	}

	/** Callback function called when a call transfer is successfully completed */
	public void onCallTransferSuccess(ExtendedCall call, Message notify) {
		printLog("onCallTransferSuccess()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("Transfer successed", LogLevel.HIGH);
		android.util.Log.v("UA", "hang up 4");
		call.hangup();
	}

	/**
	 * Callback function called when a call transfer is NOT sucessfully
	 * completed
	 */
	public void onCallTransferFailure(ExtendedCall call, String reason,
			Message notify) {
		printLog("onCallTransferFailure()", LogLevel.LOW);
		if (call != this.call) {
			printLog("NOT the current call", LogLevel.LOW);
			return;
		}
		printLog("Transfer failed", LogLevel.HIGH);
	}

	// ************************* Schedule events ***********************

	/** Schedules a re-inviting event after <i>delay_time</i> secs. */
	void reInvite(final String contact_url, final int delay_time) {
		SessionDescriptor sdp = new SessionDescriptor(local_session);
		sdp.IncrementOLine();
		final SessionDescriptor new_sdp;
		if (statusIs(UserAgent.UA_STATE_INCALL)) { // modified
			new_sdp = new SessionDescriptor(
					sdp.getOrigin(), sdp.getSessionName(), new ConnectionField(
							"IP4", "0.0.0.0"), new TimeField());
		} else {
			new_sdp = new SessionDescriptor(
					sdp.getOrigin(), sdp.getSessionName(), new ConnectionField(
							"IP4", IpAddress.localIpAddress), new TimeField());
		}
		new_sdp.addMediaDescriptors(sdp.getMediaDescriptors());
		local_session = sdp.toString();
		(new Thread() {
			public void run() {
				runReInvite(contact_url, new_sdp.toString(), delay_time);
			}
		}).start();
	}

	/** Re-invite. */
	private void runReInvite(String contact, String body, int delay_time) {
		try {
			if (delay_time > 0)
				Thread.sleep(delay_time * 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
			printLog("RE-INVITING/MODIFYING");
			if (call != null && call.isOnCall()) {
				printLog("REFER/TRANSFER");
				call.modify(contact, body);
			}
	}

	/** Schedules a call-transfer event after <i>delay_time</i> secs. */
	void callTransfer(final String transfer_to, final int delay_time) {
		// in case of incomplete url (e.g. only 'user' is present), try to
		// complete it
		final String target_url;
		if (transfer_to.indexOf("@") < 0)
			target_url = transfer_to + "@" + realm; // modified
		else
			target_url = transfer_to;
		(new Thread() {
			public void run() {
				runCallTransfer(target_url, delay_time);
			}
		}).start();
	}

	/** Call-transfer. */
	private void runCallTransfer(String transfer_to, int delay_time) {
		try {
			if (delay_time > 0)
				Thread.sleep(delay_time * 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
			if (call != null && call.isOnCall()) {
				printLog("REFER/TRANSFER");
				call.transfer(transfer_to);
			}
	}

	/** Schedules an automatic answer event after <i>delay_time</i> secs. */
	void automaticAccept(final int delay_time) {
		(new Thread() {
			public void run() {
				runAutomaticAccept(delay_time);
			}
		}).start();
	}

	/** Automatic answer. */
	private void runAutomaticAccept(int delay_time) {
		try {
			if (delay_time > 0)
				Thread.sleep(delay_time * 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
			if (call != null) {
				printLog("AUTOMATIC-ANSWER");
				accept();
			}
	}

	/** Schedules an automatic hangup event after <i>delay_time</i> secs. */
	void automaticHangup(final int delay_time) {
		(new Thread() {
			public void run() {
				runAutomaticHangup(delay_time);
			}
		}).start();
	}

	/** Automatic hangup. */
	private void runAutomaticHangup(int delay_time) {
		try {
			if (delay_time > 0)
				Thread.sleep(delay_time * 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
			if (call != null && call.isOnCall()) {
				printLog("AUTOMATIC-HANGUP");
				android.util.Log.v("UA", "hang up 5");
				hangup();
			}

	}

	// ****************************** Logs *****************************

	/** Adds a new string to the default Log */
	void printLog(String str) {
		printLog(str, LogLevel.HIGH);
	}

	/** Adds a new string to the default Log */
	void printLog(String str, int level) {
		/*if (Sipdroid.release) return;
		if (log != null)
			log.println("UA: " + str, level + SipStack.LOG_LEVEL_UA);
		if ((user_profile == null || !user_profile.no_prompt)
				&& level <= LogLevel.HIGH)
			System.out.println("UA: " + str);*/
		android.util.Log.v("SIP", str);
	}

	/** Adds the Exception message to the default Log */
	void printException(Exception e, int level) {
		/*if (Sipdroid.release) return;
		if (log != null)
			log.printException(e, level + SipStack.LOG_LEVEL_UA);*/		
		android.util.Log.v("UA", e.toString());
	}
	
	private void parse_role_notifty(String json)
	{
	    //"r":"X"//"0"-听众非视频源，"1"-发言人 非视频源，"2"-听众兼视频源，"3"-发言人兼视频源 , "9"表示关闭当前调度
		try
		{
			JSONObject root = new JSONObject(json);
			
			//参数校验
			if(false == root.has("r"))
			{
				return;
			}
			else
			{
				int role = Integer.parseInt(root.getString("r"));
				
				android.util.Log.v("SIP", "Role: " + role);//message.toString());
				
				if(9 != role)
				{ 
					MessageHandlerManager.get_instance().handle_message(GID.MSG_CHANGE_ROLE,role,GD.MEDIA_INSTANCE);
				}
				else if(9 == role)
				{
					if(UA_STATE_IDLE != SipdroidReceiver.call_state)
					{
						android.util.Log.v("SIP", "role 9 1");
						hangup();
					}
					
					if(true == GD.is_in_schedule())
					{
						android.util.Log.v("Media", "hang up 2 " + json);
						MessageHandlerManager.get_instance().handle_message(GID.MSG_HANG_UP, GD.MEDIA_INSTANCE);
					}
					else
					{
						MessageHandlerManager.get_instance().handle_message(GID.MSG_RECV_CANCEL, GD.MEDIA_INSTANCE);
					}
				}
				else if(10 == role)
				{
					if(UA_STATE_IDLE != SipdroidReceiver.call_state)
					{
						hangup();
					}
					
					MessageHandlerManager.get_instance().handle_message(GID.MSG_SCHEDULE_REJECTED, GD.MEDIA_INSTANCE);
				}
			}
		}
		catch (JSONException e) {
			// TODO Auto-generated catch block
			android.util.Log.v("JSON", e.toString());
			e.printStackTrace();
		}
	}

	//处理收到的MESSAGE信令
	@Override
	public void onReceivedMessage(SipProvider sipProvider, Message message)
	{
		// TODO Auto-generated method stub
		android.util.Log.v("SIP", "Message: " + message.getBody());
		
		if (message.isRequest() && message.isMessage())
		{
			//实现对MESSAGE消息的反馈
			/*android.util.Log.v("UA", "MESSAGE: " + message.getMethodId().toString() + ", " +
					//msg.getTransportProtocol() + ", " +
					message.getRemoteAddress() + ", " +
					message.getFromHeader().toString() + ", " +
					message.getToHeader().toString());// + ", " +
					//message.getBody());//fym*/
			
			//必须在UI线程中执行才有效 Toast.makeText(Receiver.mContext, message.getBody(), Toast.LENGTH_LONG).show();
			
			if(message.getBody().contains("roger"))//调度服务器对UA NAT注册的响应
			{
				
			}			
			else if(message.getBody().contains("{\"t\":"))//会议控制
			{
				//android.util.Log.v("SIP", "MESSAGE JSON: " + message.getBody());//message.toString());
				GD.parse_schedule_notifty_message(message.getBody());
			}
			else if(message.getBody().contains("{\"r\":"))//角色更新
			{
				//android.util.Log.v("UA", "Role MESSAGE JSON: " + message.getBody());//message.toString());
				parse_role_notifty(message.getBody());
			}
			
			TransactionServer ts = new TransactionServer(sip_provider, message,null);
			ts.respondWith(MessageFactory.createResponse(message, 200, SipResponses.reasonOf(200), null));
		}
	}
}
