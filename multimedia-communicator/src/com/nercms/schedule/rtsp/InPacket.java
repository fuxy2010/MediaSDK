package com.nercms.schedule.rtsp;

import java.nio.ByteBuffer;

public class InPacket extends Packet {
    /**
     * @param header
     * @param source
     * @param command
     * @param sequence
     * @param user
     */
    public InPacket(byte header, char source, char command) {
        super(header, source, command, (char)0);
    }
    
    /**
     * @param buf
     * @param length
     * @param user
     * @throws Exception 
     */
    public InPacket(ByteBuffer buf, int length)
            throws Exception {
        super(buf, length);
    }
    
    /**
     * @param buf
     * @param user
     * @throws Exception 
     */
    public InPacket(ByteBuffer buf){
        super(buf);
    }

    /* (non-Javadoc)
     */
	@Override
    protected boolean validateHeader() {
        return true;
    }

	@Override
	protected byte[] getBodyBytes(ByteBuffer buf, int length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getFamily() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getHeadLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getLength(int bodyLength) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getTailLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void parseBody(ByteBuffer buf) throws PacketParseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void parseHeader(ByteBuffer buf) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void parseTail(ByteBuffer buf) throws PacketParseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void putBody(ByteBuffer buf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void putHead(ByteBuffer buf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void putTail(ByteBuffer buf) {
		// TODO Auto-generated method stub
		
	}
}
