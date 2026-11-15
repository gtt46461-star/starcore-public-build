package com.ethk3.novaonepro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/** Android-runtime regression for the R940 root hierarchy and repeated scroll path. */
@RunWith(AndroidJUnit4.class)
public final class R940UiStabilityTest {
    @Test
    public void boundedDetailSurvivesActivityRecreation() throws Exception {
        String body = "R941_SELECTED_DETAIL_原始内容😀";
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), DiagnosisActivity.class)
                .putExtra("title", "原始详情").putExtra("body", body).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try (ActivityScenario<DiagnosisActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> assertEquals(body,
                    ((TextView)activity.findViewById(R.id.txt_diagnosis_full)).getText().toString()));
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals(body,
                    ((TextView)activity.findViewById(R.id.txt_diagnosis_full)).getText().toString()));
        }
    }

    @Test
    public void largeDetailUsesBoundedFilePreviewAfterRecreation() throws Exception {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        String body = "R941_LONG_DETAIL_" + "算法😀".repeat(500000);
        File file = DiagnosticFileStoreR941.write(new File(context.getCacheDir(), "r923_reports"), body);
        Intent intent = new Intent(context, DiagnosisActivity.class).putExtra("report_file", file.getName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        assertFalse(intent.hasExtra("body"));
        try (ActivityScenario<DiagnosisActivity> scenario = ActivityScenario.launch(intent)) {
            awaitPreview(scenario);
            scenario.recreate();
            awaitPreview(scenario);
        } finally { file.delete(); }
    }

    private static void awaitPreview(ActivityScenario<DiagnosisActivity> scenario) throws Exception {
        AtomicBoolean ready = new AtomicBoolean();
        for (int i = 0; i < 50 && !ready.get(); i++) {
            scenario.onActivity(activity -> {
                String text = ((TextView)activity.findViewById(R.id.txt_diagnosis_full)).getText().toString();
                if (text.contains("R941_LONG_DETAIL_")) {
                    assertTrue(text.length() < 17000); ready.set(true);
                }
            });
            if (!ready.get()) Thread.sleep(100L);
        }
        assertTrue("file preview never completed", ready.get());
    }

    @Test
    public void diagnosticPanelKeepsSingleScrollRootAndSurvivesRepeatedScroll() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(5000L);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(
                    activity -> {
                        View content = activity.findViewById(android.R.id.content);
                        assertTrue(content instanceof ViewGroup);
                        ViewGroup group = (ViewGroup) content;
                        assertEquals("activity root must never be detached", 1, group.getChildCount());
                        assertTrue("existing scroll root must remain canonical", group.getChildAt(0) instanceof ScrollView);
                        try {
                            Field installed = MainActivity.class.getDeclaredField("r940DiagnosticPanelInstalled");
                            installed.setAccessible(true);
                            assertTrue("diagnostic panel did not finish installing", installed.getBoolean(activity));
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                        assertFalse(activity.isFinishing());
                    });
            // Give Android a rendering opportunity between scrolls; forty calls in one
            // main-thread callback exercise only the final position, not forty frames.
            for (int i = 0; i < 40; i++) {
                final boolean bottom = (i & 1) == 0;
                scenario.onActivity(activity -> {
                    ViewGroup content = activity.findViewById(android.R.id.content);
                    ScrollView scroll = (ScrollView) content.getChildAt(0);
                    scroll.scrollTo(0, bottom
                            ? Math.max(0, scroll.getChildAt(0).getHeight() - scroll.getHeight()) : 0);
                    assertFalse(activity.isFinishing());
                });
                Thread.sleep(50L);
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
    }
}
