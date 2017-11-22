package com.nercms.schedule.rtsp;

import java.nio.ByteBuffer;

import android.util.Log;

public abstract class Packet {

	protected static final ByteBuffer bodyBuf = ByteBuffer.allocate(EnginConst.QQ_MAX_PACKET_SIZE);

	protected char command;

	protected char source;

	protected char sequence;

	protected byte header;

	protected byte[] body;

	public Packet(byte header, char source, char command, char sequence) {
		this.header = header;
		this.source = source;
		this.command = command;
		this.sequence = sequence;
	}

	public Packet(ByteBuffer buf){
		body = new byte[buf.limit()];
		buf.get(body);
	}

	protected Packet(ByteBuffer buf, int length){
	}

	protected abstract int getLength(int bodyLength);

	protected abstract boolean validateHeader();

	protected abstract int getHeadLength();

	protected abstract int getTailLength();

	protected abstract void putHead(ByteBuffer buf);

	protected abstract void putBody(ByteBuffer buf);

	protected abstract byte[] getBodyBytes(ByteBuffer buf, int length);

	public abstract int getFamily();

	protected abstract void putTail(ByteBuffer buf);

	protected abstract void parseBody(ByteBuffer buf)
			throws PacketParseException;

	protected abstract void parseHeader(ByteBuffer buf)
			throws Exception;

	protected abstract void parseTail(ByteBuffer buf)
			throws PacketParseException;

	public String toString() {
		return "Packet name: " + getPacketName() + " Packet Seriesline: " + (int) sequence;
	}

	public String toDebugString() {
		return "toDebugString not implemented!";
	}

	public boolean equals(Object obj) {
		if (obj instanceof Packet) {
			Packet packet = (Packet) obj;
			return header == packet.header && command == packet.command
					&& sequence == packet.sequence;
		} else
			return super.equals(obj);
	}
	
	/**
	 * 把序列号和命令拼起来作为哈希码. 为了避免不同header的包有相同的命令，Header也参与进来
	 */
	public int hashCode() {
		return hash(sequence, command);
	}

	public static int hash(char sequence, char command) {
		return (sequence << 16) | command;
	}

	/**
	 * @return Returns the command.
	 */
	public char getCommand() {
		return command;
	}

	/**
	 * @return Returns the sequence.
	 */
	public char getSequence() {
		return sequence;
	}

	/**
	 * @param sequence
	 *            The sequence to set.
	 */
	public void setSequence(char sequence) {
		this.sequence = sequence;
	}

	public String getPacketName() {
		return "Unknown Packet";
	}

	/**
	 * @return Returns the source.
	 */
	public char getSource() {
		return source;
	}

	/**
	 * @return Returns the header.
	 */
	public byte getHeader() {
		return header;
	}

	/**
	 * @param header
	 *            The header to set.
	 */
	public void setHeader(byte header) {
		this.header = header;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

}