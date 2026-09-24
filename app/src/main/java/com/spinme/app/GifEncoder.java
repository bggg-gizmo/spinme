package com.spinme.app;

import android.graphics.Bitmap;
import java.io.*;

public final class GifEncoder implements Closeable {
    private final OutputStream out;
    private final int width, height, delayCs;
    private boolean closed;

    public GifEncoder(OutputStream out, int width, int height, int fps) throws IOException {
        this.out = new BufferedOutputStream(out, 64*1024);
        this.width = width; this.height = height;
        this.delayCs = Math.max(1, Math.round(100f / Math.max(1, fps)));
        writeHeader();
    }

    private void writeHeader() throws IOException {
        out.write("GIF89a".getBytes("US-ASCII"));
        le16(width); le16(height);
        out.write(0xF7); // GCT, 8-bit resolution, 256 entries
        out.write(0); out.write(0);
        for (int i=0;i<256;i++) {
            int r=(i>>5)&7, g=(i>>2)&7, b=i&3;
            out.write(r*255/7); out.write(g*255/7); out.write(b*255/3);
        }
        // NETSCAPE infinite loop extension
        out.write(new byte[]{0x21,(byte)0xFF,0x0B});
        out.write("NETSCAPE2.0".getBytes("US-ASCII"));
        out.write(new byte[]{0x03,0x01,0x00,0x00,0x00});
    }

    public void addFrame(Bitmap bitmap) throws IOException {
        if (closed) throw new IOException("encoder closed");
        Bitmap b = bitmap;
        if (bitmap.getWidth()!=width || bitmap.getHeight()!=height)
            b = Bitmap.createScaledBitmap(bitmap,width,height,true);
        int[] pixels = new int[width*height];
        b.getPixels(pixels,0,width,0,0,width,height);
        byte[] idx = new byte[pixels.length];
        for (int i=0;i<pixels.length;i++) {
            int c=pixels[i];
            int r=(c>>16)&255, g=(c>>8)&255, bl=c&255;
            idx[i]=(byte)(((r>>5)<<5)|((g>>5)<<2)|(bl>>6));
        }
        if (b != bitmap) b.recycle();
        out.write(new byte[]{0x21,(byte)0xF9,0x04,0x04});
        le16(delayCs); out.write(0); out.write(0);
        out.write(0x2C); le16(0); le16(0); le16(width); le16(height); out.write(0);
        out.write(8); // LZW min code size
        writeLiteralLzw(idx);
    }

    // Valid GIF LZW stream using 9-bit literal codes and periodic CLEAR codes.
    // Clearing before the dictionary reaches 512 keeps the code width at 9 bits.
    private void writeLiteralLzw(byte[] idx) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream(idx.length*2);
        BitWriter bw = new BitWriter(data);
        final int CLEAR=256, EOF=257;
        int p=0;
        while (p<idx.length) {
            bw.write(CLEAR,9);
            int end=Math.min(idx.length,p+250);
            while (p<end) bw.write(idx[p++]&255,9);
        }
        bw.write(EOF,9); bw.flush();
        byte[] bytes=data.toByteArray();
        for (int i=0;i<bytes.length;i+=255) {
            int n=Math.min(255,bytes.length-i); out.write(n); out.write(bytes,i,n);
        }
        out.write(0);
    }

    private void le16(int v) throws IOException { out.write(v&255); out.write((v>>>8)&255); }
    @Override public void close() throws IOException { if (!closed) { closed=true; out.write(0x3B); out.flush(); } }

    private static final class BitWriter {
        final OutputStream out; int buffer,bits;
        BitWriter(OutputStream out){this.out=out;}
        void write(int code,int n) throws IOException {
            buffer |= (code & ((1<<n)-1)) << bits; bits += n;
            while(bits>=8){ out.write(buffer&255); buffer >>>=8; bits-=8; }
        }
        void flush() throws IOException { if(bits>0){out.write(buffer&255);buffer=bits=0;} }
    }
}
