package edu.nau.siccs;

import javax.smartcardio.*;
import java.nio.charset.StandardCharsets;

public class Main {
    private final static byte CLA = (byte)0x80;
    private final static byte[] AID = new byte[] { (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x01 };

    private final static byte INS_ECHO = 0x01;
    private final static byte INS_TRANSPOSE = 0x02;

    public static void main(String[] args) {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminals terminals = factory.terminals();

        System.out.println("Waiting for cards...");

        while (true) {
            try {
                for (CardTerminal terminal : terminals.list(CardTerminals.State.CARD_INSERTION)) {
                    Card card = terminal.connect("*");
                    CardChannel channel = card.getBasicChannel();
                    try {
                        select(channel);

                        String message = "Hello, world!";
                        String response = echoMessage(channel, message);

                        System.out.println();
                        System.out.println("Echo Example");
                        System.out.printf("Your message: %s\n", message);
                        System.out.printf("Response message: %s\n",  response);

                        response = reverseMessage(channel, message);

                        System.out.println();
                        System.out.println("Reverse Example");
                        System.out.printf("Your message: %s\n", message);
                        System.out.printf("Response message: %s\n",  response);

                        channel.close();
                    } catch (EchoException err) {
                        System.err.println(err.getMessage());
                    }
                }

                terminals.waitForChange();
            } catch (CardException err) {
                err.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static void select(CardChannel channel) throws CardException, EchoException {
        CommandAPDU apdu = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, AID);
        ResponseAPDU resp = channel.transmit(apdu);

        if(resp.getSW() != Status.SUCCESS.CODE) {
            throw new EchoException(String.format("Selecting applet unexpectedly failed. SW: 0x%04X", resp.getSW()));
        }
    }

    private static String echoMessage(CardChannel channel, String message) throws CardException, EchoException {
        CommandAPDU apdu = new CommandAPDU(CLA, INS_ECHO, 0x00, 0x00, message.getBytes(StandardCharsets.US_ASCII));
        ResponseAPDU resp = channel.transmit(apdu);

        if(resp.getSW() != Status.SUCCESS.CODE) {
            throw new EchoException(String.format("Selecting applet unexpectedly failed. SW: 0x%04X", resp.getSW()));
        }

        // Read the response data as ASCII bytes.
        return new String(resp.getData(), StandardCharsets.US_ASCII);
    }

    private static String reverseMessage(CardChannel channel, String message) throws CardException, EchoException {
        CommandAPDU apdu = new CommandAPDU(CLA, INS_TRANSPOSE, 0x00, 0x00, message.getBytes(StandardCharsets.US_ASCII));
        ResponseAPDU resp = channel.transmit(apdu);

        if(resp.getSW() != Status.SUCCESS.CODE) {
            throw new EchoException(String.format("Selecting applet unexpectedly failed. SW: 0x%04X", resp.getSW()));
        }

        // Read the response data as ASCII bytes.
        return new String(resp.getData(), StandardCharsets.US_ASCII);
    }
}
