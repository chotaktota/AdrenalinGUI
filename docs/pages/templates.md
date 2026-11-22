# Шаблоны иконок

Повторяющееся оформление объявляется один раз. Различающиеся значения передаются в iconAt.

## Без параметров

~~~kotlin
template("border") {
    type(Material.RED_STAINED_GLASS_PANE)
    name(" ")
    allFlags()
}

iconAt(pos('A'), template("border"))
~~~

## Два параметра

~~~kotlin
val warp = template("warp", Material::class.java, String::class.java) { material, label ->
    type(material)
    name("<white>" + label)
    loreLine("<red>Нажмите, чтобы перейти")
    allFlags()
}

iconAt(pos('X'), warp, Material.GRASS_BLOCK, "Спавн") {
    onClick { player.performCommand("spawn") }
}

iconAt(pos('Y'), warp, Material.DIAMOND_PICKAXE, "Шахта") {
    onClick { player.performCommand("mine") }
}
~~~

## Один параметр

~~~kotlin
val category = template("category", String::class.java) { label ->
    type(Material.BOOK)
    name("<red>" + label)
}

iconAt(pos('C'), category, "Механики")
~~~

Шаблоны принадлежат Pattern, в котором объявлены. Дополнительный блок iconAt выполняется после оформления шаблона.
