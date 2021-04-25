package edu.nau.siccs;

public enum Status {
    // See for more status words https://www.eftlab.co.uk/index.php/site-map/knowledge-base/118-apdu-response-list
    SUCCESS(0x9000), INTERNAL_ERROR(0x6F00), INS_NOT_SUPPORT(0x6D00),
    WRONG_LENGTH(0x6700);

    public final int CODE;

    Status(int code) {
        this.CODE = code;
    }
}
