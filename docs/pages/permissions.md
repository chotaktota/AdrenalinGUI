# Права и группы LuckPerms

Проверяйте доступ игрока, читайте метаданные и изменяйте права и группы через действия.

## Проверки и метаданные

~~~kotlin
onClick {
    if (hasPermission("shop.vip")) {
        player.sendMessage("Раздел доступен")
    }
    val group = primaryGroup()
    val prefix = prefix()
    val groups = groups()
}
~~~

hasPermission(String) использует Bukkit-проверку игрока. prefix, suffix, groups и primaryGroup читают данные LuckPerms.

Перегрузка hasPermission(player, permission) использует контекстную проверку LuckPerms и может вернуть null.

## Изменение прав и групп

~~~kotlin
onClick {
    setPermission("shop.vip", duration = Duration.ofDays(7))
    addGroup("vip", duration = Duration.ofDays(7))
}
~~~

~~~kotlin
onClick {
    unsetPermission("shop.vip")
    removeGroup("vip")
}
~~~

Доступны перегрузки с UUID для другого пользователя. Временные права и группы требуют положительный Duration.

## Обработка результата

Изменения возвращают CompletableFuture<Void>. Продолжение может выполняться вне основного потока.

~~~kotlin
onClick {
    val viewer = player
    val future = setPermission("shop.vip")
    future.whenComplete { _, error ->
        plugin.server.scheduler.runTask(plugin, Runnable {
            if (viewer.isOnline) {
                viewer.sendMessage(
                    if (error == null) "Право сохранено"
                    else "Не удалось сохранить право"
                )
            }
        })
    }
}
~~~

Перед Bukkit-вызовами переключайтесь на основной поток. Для более сложных операций доступны user, loadUser, cleanupUser, modifyUser и api.
