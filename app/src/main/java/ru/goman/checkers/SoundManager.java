package ru.goman.checkers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import androidx.annotation.NonNull;

/**
 * Централизованный менеджер коротких звуков (клик кнопки и т.п.).
 */
public final class SoundManager {

    private static volatile SoundManager instance;

    private final SoundPool soundPool;
    private final int clickSoundId;
    private boolean clickLoaded = false;

    private SoundManager(@NonNull Context appContext) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();

        // ВАЖНО: файл res/raw/click.wav (или .ogg)
        clickSoundId = soundPool.load(appContext, R.raw.click, 1);

        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0 && sampleId == clickSoundId) {
                clickLoaded = true;
            }
        });
    }

    /**
     * Ленивая инициализация.
     */
    @NonNull
    public static SoundManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (SoundManager.class) {
                if (instance == null) {
                    Context appCtx = context.getApplicationContext();
                    instance = new SoundManager(appCtx);
                }
            }
        }
        return instance;
    }

    /**
     * Проиграть звук клика кнопки.
     */
    public void playClick() {
        if (!clickLoaded) {
            return;
        }
        // leftVolume, rightVolume, priority, loop, rate
        soundPool.play(clickSoundId, 1f, 1f, 1, 0, 1f);
    }

    /**
     * Освобождение ресурсов (если когда-нибудь понадобится).
     */
    public void release() {
        soundPool.release();
        instance = null;
    }
}
