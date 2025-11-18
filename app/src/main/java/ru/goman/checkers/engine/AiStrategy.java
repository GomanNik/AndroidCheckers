package ru.goman.checkers.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ru.goman.checkers.logic.GameLogic;
import ru.goman.checkers.model.Move;
import ru.goman.checkers.model.Player;

/**
 * Стратегия выбора хода для ИИ.
 * Абсолютно независима от Android / UI, работает только с GameLogic.
 */
public interface AiStrategy {

    /**
     * Выбрать ход ИИ в текущем положении.
     *
     * @param logic      текущая логика игры (живой объект).
     *                   Стратегия обязана восстанавливать состояние через снапшоты,
     *                   чтобы после выхода из метода позиция НЕ изменилась.
     * @param aiPlayer   цвет, за который играет ИИ (WHITE / BLACK).
     * @param difficulty уровень сложности.
     * @return выбранный ход или null, если допустимых ходов нет.
     */
    @Nullable
    Move chooseMove(@NonNull GameLogic logic,
                    @NonNull Player aiPlayer,
                    @NonNull AiDifficulty difficulty);
}
