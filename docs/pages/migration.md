# Переход на API 2.x

Пересоберите использующие API плагины и обновите явные импорты. Действия доступны непосредственно в onClick.

## Действия

| Ранее | Теперь |
| --- | --- |
| PlayerItems, items.count(...) | countItem(...) |
| items.give(...), items.take(...) | giveItem(...), takeItem(...) |
| items.has(...), items.canFit(...) | hasItem(...), canFitItem(...) |
| MenuEconomy | PlayerAction |
| MenuPermissions | PlayerAction |
| ChatInput | OtherAction.Chat |
| ChatInputFailure | OtherAction.InputFailure |

Реализации находятся в ItemAction, PlayerAction и OtherAction в пакете dev.aethereal.agui.surface.item.click.action.

## Пакеты

| Назначение | Пакет |
| --- | --- |
| Поверхности | dev.aethereal.agui.surface |
| Билдеры предметов | dev.aethereal.agui.surface.item.builder |
| Иконки и поля | dev.aethereal.agui.surface.item |
| Контекст клика | dev.aethereal.agui.surface.item.click |
| Страницы | dev.aethereal.agui.surface.layout.page |
| Шаблоны | dev.aethereal.agui.surface.layout |
| Движок и реестр | dev.aethereal.agui.engine |

## Кэш и открытые окна

Описание меню кэшируется на экземпляре поверхности. Для значений игрока используйте dynamic и плейсхолдеры, для изменения открытого окна — refresh.

invalidateCache влияет на последующие открытия. Ранее открытая сессия продолжает использовать свой построенный шаблон.
