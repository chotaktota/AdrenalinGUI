# Экономика Vault

balance, hasMoney, withdraw и deposit доступны прямо в обработчике клика. Нужны Vault и экономический провайдер.

## Баланс и проверка

~~~kotlin
onClick {
    val amount = balance()
    player.sendMessage("Баланс: " + format(amount))
    if (!hasMoney(100.0)) {
        sound(Sound.ENTITY_VILLAGER_NO)
    }
}
~~~

## Списание и пополнение

~~~kotlin
onClick {
    val payment = withdraw(100.0)
    if (payment.success) {
        player.sendMessage("Списано: " + payment.amount)
        sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
    } else {
        player.sendMessage("Списание отклонено")
    }
}
~~~

| Поле PlayerAction.Transaction | Содержимое |
| --- | --- |
| success | Успешность операции |
| amount | Сумма из ответа провайдера |
| error | Сообщение провайдера об ошибке |

deposit(amount) использует такой же результат. Проверяйте результат самой транзакции. hasMoney показывает состояние в момент проверки.

Сумма должна быть конечной и неотрицательной.

## Внешние обращения

~~~kotlin
val actions = engine.actions(player)
val payment = actions.withdraw(100.0)
val balance = engine.economy.balance(offlinePlayer)
~~~

Внешние обращения выполняются на основном потоке.

## Покупка предмета

В своём обработчике проверьте canFitItem до списания, результат withdraw после списания и результат giveItem при выдаче. Если выдача не удалась, обработайте возврат через deposit и проверьте его success.

Связь транзакции с покупкой, повторные попытки и компенсации определяет ваш плагин.
