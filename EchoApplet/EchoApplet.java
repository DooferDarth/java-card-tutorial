package edu.nau.siccs;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

public class EchoApplet extends Applet {
	private byte[] echoMessage;
	private short msgLength;
	
	// Equivalent to the max APDU response data length without Extended Length APDU support enabled.
	private static final short MAX_MESSAGE_LENGTH = 256;
	
	// The instruction byte for ECHO is the value 0x01.
	private static final byte INS_ECHO = 0x01;
	private static final byte INS_TRANSPOSE = 0x02;

	private EchoApplet (byte[] bArray, short bOffset, byte bLength) {
		// Initialize the message in RAM since we don't need to carry the data over, and is much faster than using the EEPROM.
		this.echoMessage = JCSystem.makeTransientByteArray(MAX_MESSAGE_LENGTH, JCSystem.CLEAR_ON_RESET);
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		(new EchoApplet(bArray, bOffset, bLength)).register();
	}

	public void process(APDU apdu) {
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		// Decide which action to do based on what the OFFSET_INS byte is set to.
		switch (buf[ISO7816.OFFSET_INS]) {
			case (byte)0x00:
				break;
			case INS_ECHO:
				readMessage(apdu);
				sendMessage(apdu);
				break;
			case INS_TRANSPOSE:
				readMessage(apdu);
				reverse(this.echoMessage, this.msgLength, (short)0);
				sendMessage(apdu);
				break;
			// If it's none of the options, then assume the instruction is not supported.
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	public void readMessage(APDU apdu) {
		byte buffer[] = apdu.getBuffer();
		short bytesRead = apdu.setIncomingAndReceive();

		short size = apdu.getIncomingLength();
		// If size is too great, then throw back an error.
		if(size > MAX_MESSAGE_LENGTH) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
		
		// Start reading the message chunks at a time.
		short offset = (short) 0;
		while (bytesRead > 0) {
			Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, this.echoMessage,
					offset, bytesRead);
			offset += bytesRead;
			bytesRead = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}
		
		// Set the message length so that sendMessage knows what to set the outgoing message length to.
		this.msgLength = size;
	}
	
	// Must make sure that length + offset won't overflow or else j will be negative
	// (since Java integers aren't unsigned)
	public void reverse(byte[] array, short length, short offset) {
		for(short i = offset, j = (short)(length + offset - 1); i < j; i++, j--) {
			byte tmp = array[i];
			array[i] = array[j];
			array[j] = tmp;
		}
	}
	
	public void sendMessage(APDU apdu) {
		apdu.setOutgoing();
		// Set message length
		apdu.setOutgoingLength(this.msgLength);
		
		// Send msgLength bytes from echoMessage back.
		apdu.sendBytesLong(this.echoMessage, (short)0, this.msgLength);
	}
	
}