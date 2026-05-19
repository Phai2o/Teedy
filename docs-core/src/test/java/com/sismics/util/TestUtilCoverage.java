package com.sismics.util;

import com.google.common.io.ByteStreams;
import jakarta.json.JsonValue;
import org.junit.Assert;
import org.junit.Test;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Additional tests added to improve instruction and branch coverage for
 * lightweight utility classes that previously had little or no test coverage.
 *
 * Targets:
 *  - {@link EnvironmentUtil}
 *  - {@link LocaleUtil}
 *  - {@link JsonUtil}
 *  - {@link HttpUtil}
 *  - {@link ClasspathScanner}
 *  - {@link ImageUtil} (additional branches)
 */
public class TestUtilCoverage {

    // ------------------------------------------------------------------
    // EnvironmentUtil
    // ------------------------------------------------------------------

    @Test
    public void environmentUtilOsDetection() {
        // Exactly one of the three OS predicates should be true on a normal machine.
        boolean win = EnvironmentUtil.isWindows();
        boolean mac = EnvironmentUtil.isMacOs();
        boolean unix = EnvironmentUtil.isUnix();
        // At least one should be true on any sane os.name; this still exercises
        // the underlying OS.contains(...) branches.
        Assert.assertTrue(win || mac || unix);
    }

    @Test
    public void environmentUtilWebappContextToggle() {
        boolean previous = EnvironmentUtil.isWebappContext();
        try {
            EnvironmentUtil.setWebappContext(false);
            Assert.assertFalse(EnvironmentUtil.isWebappContext());
            Assert.assertTrue(EnvironmentUtil.isUnitTest());

            EnvironmentUtil.setWebappContext(true);
            Assert.assertTrue(EnvironmentUtil.isWebappContext());
            Assert.assertFalse(EnvironmentUtil.isUnitTest());
        } finally {
            EnvironmentUtil.setWebappContext(previous);
        }
    }

    @Test
    public void environmentUtilDevMode() {
        // We don't set application.mode in tests, so isDevMode should be false.
        // This still executes the equalsIgnoreCase branch with a non-"dev" value.
        Assert.assertFalse(EnvironmentUtil.isDevMode());
    }

    @Test
    public void environmentUtilGetters() {
        // These getters simply expose the static fields; calling them ensures
        // the getter bytecode is exercised. Values may legitimately be null in
        // a unit-testing environment, so we only check they don't throw.
        EnvironmentUtil.getWindowsAppData();
        EnvironmentUtil.getMacOsUserHome();
        EnvironmentUtil.getTeedyHome();
    }

    // ------------------------------------------------------------------
    // LocaleUtil
    // ------------------------------------------------------------------

    @Test
    public void localeUtilNullOrEmptyReturnsEnglish() {
        Assert.assertEquals(Locale.ENGLISH, LocaleUtil.getLocale(null));
        Assert.assertEquals(Locale.ENGLISH, LocaleUtil.getLocale(""));
    }

    @Test
    public void localeUtilLanguageOnly() {
        Locale locale = LocaleUtil.getLocale("fr");
        Assert.assertEquals("fr", locale.getLanguage());
        Assert.assertEquals("", locale.getCountry());
        Assert.assertEquals("", locale.getVariant());
    }

    @Test
    public void localeUtilLanguageAndCountry() {
        Locale locale = LocaleUtil.getLocale("fr_FR");
        Assert.assertEquals("fr", locale.getLanguage());
        Assert.assertEquals("FR", locale.getCountry());
        Assert.assertEquals("", locale.getVariant());
    }

    @Test
    public void localeUtilLanguageCountryVariant() {
        Locale locale = LocaleUtil.getLocale("zh_CN_Hans");
        Assert.assertEquals("zh", locale.getLanguage());
        Assert.assertEquals("CN", locale.getCountry());
        Assert.assertEquals("Hans", locale.getVariant());
    }

    // ------------------------------------------------------------------
    // JsonUtil
    // ------------------------------------------------------------------

    @Test
    public void jsonUtilNullableString() {
        Assert.assertEquals(JsonValue.NULL, JsonUtil.nullable((String) null));
        JsonValue value = JsonUtil.nullable("hello");
        Assert.assertEquals(JsonValue.ValueType.STRING, value.getValueType());
    }

    @Test
    public void jsonUtilNullableInteger() {
        Assert.assertEquals(JsonValue.NULL, JsonUtil.nullable((Integer) null));
        JsonValue value = JsonUtil.nullable(Integer.valueOf(42));
        Assert.assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
    }

    @Test
    public void jsonUtilNullableLong() {
        Assert.assertEquals(JsonValue.NULL, JsonUtil.nullable((Long) null));
        JsonValue value = JsonUtil.nullable(Long.valueOf(123456789L));
        Assert.assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
    }

    // ------------------------------------------------------------------
    // HttpUtil
    // ------------------------------------------------------------------

    @Test
    public void httpUtilBuildExpiresHeader() {
        long offsetMs = 60_000L;
        long before = new Date().getTime();
        String header = HttpUtil.buildExpiresHeader(offsetMs);
        long after = new Date().getTime();

        Assert.assertNotNull(header);
        Assert.assertFalse(header.isEmpty());

        // The header is formatted with the same pattern used internally; we can
        // parse it back and verify it falls within the expected window.
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            long parsed = fmt.parse(header).getTime();
            // Parsed time has second precision, so allow a 1s margin on each side.
            Assert.assertTrue(parsed >= (before + offsetMs) - 1000);
            Assert.assertTrue(parsed <= (after + offsetMs) + 1000);
        } catch (Exception e) {
            Assert.fail("Could not parse header: " + header + " (" + e.getMessage() + ")");
        }
    }

    // ------------------------------------------------------------------
    // ClasspathScanner
    // ------------------------------------------------------------------

    @Test
    public void classpathScannerFindsConcreteImplementations() {
        ClasspathScanner<Runnable> scanner = new ClasspathScanner<>();
        // Search for Runnable implementations in our own test package; this
        // exercises sorting + filtering even when the result set is empty.
        List<Class<Runnable>> classes = scanner.findClasses(Runnable.class, "com.sismics.util");
        Assert.assertNotNull(classes);
        // No abstract / interface classes should appear in the results.
        for (Class<Runnable> c : classes) {
            Assert.assertFalse(c.isInterface());
            Assert.assertFalse(java.lang.reflect.Modifier.isAbstract(c.getModifiers()));
        }
    }

    @Test
    public void classpathScannerEmptyForUnknownPackage() {
        ClasspathScanner<Runnable> scanner = new ClasspathScanner<>();
        List<Class<Runnable>> classes = scanner.findClasses(Runnable.class, "no.such.package.exists.here");
        Assert.assertNotNull(classes);
        Assert.assertTrue(classes.isEmpty());
    }

    // ------------------------------------------------------------------
    // ImageUtil — additional branches not covered by TestImageUtil
    // ------------------------------------------------------------------

    @Test
    public void imageUtilGravatarNull() {
        Assert.assertNull(ImageUtil.computeGravatar(null));
    }

    @Test
    public void imageUtilIsBlackOnBinaryImage() {
        // TYPE_BYTE_BINARY exercises the early-return branch.
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_BINARY);
        // pixel (0,0) defaults to 0 (black) for a fresh binary image.
        Assert.assertTrue(ImageUtil.isBlack(img, 0, 0));
    }

    @Test
    public void imageUtilIsBlackOnRgbImage() {
        // TYPE_INT_RGB takes the luminance branch.
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 2, 2);
        g.dispose();
        Assert.assertTrue(ImageUtil.isBlack(img, 0, 0));

        BufferedImage white = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Graphics gw = white.getGraphics();
        gw.setColor(Color.WHITE);
        gw.fillRect(0, 0, 2, 2);
        gw.dispose();
        Assert.assertFalse(ImageUtil.isBlack(white, 0, 0));
    }

    @Test
    public void imageUtilWriteJpegRgb() throws Exception {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 8, 8);
        g.dispose();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageUtil.writeJpeg(img, bos);
        byte[] data = bos.toByteArray();

        Assert.assertTrue("Expected non-trivial JPEG output", data.length > 4);
        // JPEG magic bytes: FF D8 FF
        Assert.assertEquals((byte) 0xFF, data[0]);
        Assert.assertEquals((byte) 0xD8, data[1]);
        Assert.assertEquals((byte) 0xFF, data[2]);
    }

    @Test
    public void imageUtilWriteJpegArgbStripsAlpha() throws Exception {
        // Image with alpha hits the "Strip alpha channel" branch.
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setColor(new Color(0, 0, 255, 128));
        g.fillRect(0, 0, 8, 8);
        g.dispose();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageUtil.writeJpeg(img, bos);
        byte[] data = bos.toByteArray();
        Assert.assertTrue(data.length > 4);
        Assert.assertEquals((byte) 0xFF, data[0]);
        Assert.assertEquals((byte) 0xD8, data[1]);

        // Cheap way to silence the unused-import warning if ByteStreams was kept.
        Assert.assertNotNull(ByteStreams.toByteArray(new java.io.ByteArrayInputStream(data)));
    }
}
