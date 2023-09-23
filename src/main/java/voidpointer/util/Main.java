package voidpointer.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Main {
    enum Mode { ENCODE, DECODE };

    private static Mode mode;
    private static Queue<File> files;

    public static void main(String[] args) {
        if (!parseArgs(args))
            return;
        switch (mode) {
            case ENCODE -> encode();
            case DECODE -> decode();
        }
    }

    private static boolean parseArgs(final String[] args) {
        if (args.length < 1) {
            System.out.println("""
                    JPngEncoder encodes UTF-8 text to green-alpha .png files.
                    Usage:
                    jpcode [e|d] {file1} {file2} ...
                      The program fallbacks to reading stdin if there're no files provided.
                      > e - encode a given file as PNG. Will produce {filename}.png
                            or encoded-{timestamp}.png.
                      > d - decode a given PNG file. The result is written to stdout.
                    """);
            return false;
        }
        switch (args[0]) {
            case "e" -> mode = Mode.ENCODE;
            case "d" -> mode = Mode.DECODE;
            default -> {
                return false;
            }
        }
        files = new ConcurrentLinkedQueue<>();
        for (int i = 1; i < args.length; i++) {
            File file = Path.of(args[i]).toFile();
            if (!file.exists()) {
                System.err.printf("%s does not exist\n", file.toPath());
                continue;
            }
            files.add(file);
        }
        return true;
    }

    private static void decode() {
        if (files.isEmpty()) {
            try {
                System.out.println(new String(decodeImage(ImageIO.read(System.in)), UTF_8));
            } catch (final IOException ioException) {
                System.err.printf("Could not read stdin: %s\n", ioException.getMessage());
            }
            return;
        }
        final boolean printFilenames = files.size() > 1;
        File file;
        while ((file = files.poll()) != null) {
            if (printFilenames)
                System.out.printf("%s:\n", file.toPath());
            try {
                System.out.print(new String(decodeImage(ImageIO.read(file)), UTF_8));
            } catch (final IOException ioException) {
                System.err.printf("Could not read %s: %s\n", file.toPath(), ioException.getMessage());
            }
        }
    }

    private static byte[] decodeImage(final BufferedImage image) {
        ByteArrayOutputStream decodedChars = new ByteArrayOutputStream(image.getHeight() * image.getWidth());
        image_decode_loop:
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if ((pixel & 0xFF000000) == 0)
                    break image_decode_loop;
                decodedChars.write(pixel >> 8);
            }
        }
        return decodedChars.toByteArray();
    }

    private static void encode() {
        if (files.isEmpty()) {
            /* encode stdin */
            try {
                File destination = new File(System.currentTimeMillis() + ".png");
                encodeImage(System.in).ifPresent(image -> {
                    try {
                        ImageIO.write(image, "png", destination);
                        System.out.println(destination.toPath());
                    } catch (final IOException ioException) {
                        System.err.printf("Could not save encoded stdin: %s\n", ioException.getMessage());
                    }
                });
            } catch (final IOException ioException) {
                System.err.printf("Could not encode stdin: %s\n", ioException.getMessage());
            }
            return;
        }
        /* encode given files */
        while (files.peek() != null) {
            final File file = files.poll();
            final File destination = getPngDestination(file);
            try (var in = new BufferedInputStream(new FileInputStream(file))) {
                encodeImage(in).ifPresent(image -> {
                    try {
                        ImageIO.write(image, "png", destination);
                        System.out.printf("%s\t%s\n", file.toPath(), destination);
                    } catch (final IOException ioException) {
                        System.err.printf("Could not save encoded %s: %s\n",
                                file.toPath(), ioException.getMessage());
                    }
                });
            } catch (final IOException ioException) {
                System.err.printf("Could not encode %s: %s\n", file.toPath(), ioException.getMessage());
            }
        }
    }

    private static File getPngDestination(final File file) {
        final String filename = file.getName();
        final String filenameWithoutExtension;
        final int indexOfExtension = filename.lastIndexOf('.');
        if (indexOfExtension < 1 || filename.substring(indexOfExtension).equals(".png"))
            filenameWithoutExtension = filename;
        else
            filenameWithoutExtension = filename.substring(0, indexOfExtension);
        return Path.of(filenameWithoutExtension + ".png").toFile();
    }

    private static Optional<BufferedImage> encodeImage(final InputStream in)
            throws IOException {
        byte[] inputBytes;
        try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(in, UTF_8))) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            inputReader.lines().forEach(line -> {
                byteArrayOutputStream.writeBytes(line.getBytes(UTF_8));
                byteArrayOutputStream.write('\n');
            });
            inputBytes = byteArrayOutputStream.toByteArray();
        }
        if (inputBytes.length == 0)
            return Optional.empty();
        final int imageSide = (int) Math.ceil(Math.sqrt(inputBytes.length));
        BufferedImage image = new BufferedImage(imageSide, imageSide, BufferedImage.TYPE_INT_ARGB);
        image_render_loop:
        for (int y = 0; y < imageSide; y++) {
            for (int x = 0; x < imageSide; x++) {
                int index = x + (imageSide * y);
                if (index >= inputBytes.length)
                    break image_render_loop;
                image.setRGB(x, y, 0xFF000000 | ((inputBytes[index] & 0xFF) << 8));
            }
        }
        return Optional.of(image);
    }
}
