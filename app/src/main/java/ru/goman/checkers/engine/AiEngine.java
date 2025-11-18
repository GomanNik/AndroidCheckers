package ru.goman.checkers.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import ru.goman.checkers.logic.GameLogic;
import ru.goman.checkers.model.Move;
import ru.goman.checkers.model.Player;

/**
 * Движок ИИ.
 * Знает:
 *  - за какой цвет играет ИИ;
 *  - какие стратегии доступны;
 *  - как сопоставить уровень сложности с конкретной стратегией.
 * Сам по себе не зависит от Android и ничего не рисует.
 */
public final class AiEngine {

    @NonNull
    private final Player aiPlayer;

    @NonNull
    private final AiStrategy simpleStrategy;

    /**
     * Создаёт движок ИИ для указанного цвета.
     * По умолчанию используется SimpleAiStrategy.
     */
    public AiEngine(@NonNull Player aiPlayer) {
        this(aiPlayer, new SimpleAiStrategy());
    }

    /**
     * Конструктор с возможностью подменить стратегию (для тестов / будущих улучшений).
     */
    public AiEngine(@NonNull Player aiPlayer,
                    @NonNull AiStrategy simpleStrategy) {

        this.aiPlayer = Objects.requireNonNull(aiPlayer, "aiPlayer");
        this.simpleStrategy = Objects.requireNonNull(simpleStrategy, "simpleStrategy");
    }

    /**
     * Цвет, за который играет ИИ.
     */
    @NonNull
    public Player getAiPlayer() {
        return aiPlayer;
    }

    /**
     * Удобный вход: уровень сложности приходит как int (0..4).
     */
    @Nullable
    public Move chooseMove(@NonNull GameLogic logic, int difficultyLevel) {
        AiDifficulty difficulty = AiDifficulty.fromLevelIndex(difficultyLevel);
        return chooseMove(logic, difficulty);
    }

    /**
     * Основной метод: выбор хода для ИИ при заданной сложности.
     * Сейчас все уровни используют SimpleAiStrategy, но позже
     * здесь можно сделать switch по difficulty и подключить
     * более тяжёлые/быстрые стратегии.
     */
    @Nullable
    public Move chooseMove(@NonNull GameLogic logic,
                           @NonNull AiDifficulty difficulty) {

        Objects.requireNonNull(logic, "logic");
        Objects.requireNonNull(difficulty, "difficulty");

        // На всякий случай: если сейчас не очередь ИИ — ход не выбираем.
        if (logic.getCurrentPlayer() != aiPlayer) {
            return null;
        }

        return simpleStrategy.chooseMove(logic, aiPlayer, difficulty);
    }
}
