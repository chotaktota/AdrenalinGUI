package dev.aethereal.agui.surface.item.builder

import com.google.gson.JsonParser
import dev.aethereal.agui.engine.MenuEngine
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.inventory.meta.components.CustomModelDataComponent
import org.bukkit.inventory.meta.components.EquippableComponent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionType
import java.net.URI
import java.util.Base64
import java.util.UUID

abstract class ItemProperties internal constructor() {
    protected abstract fun editMeta(action: ItemMeta.() -> Unit)

    fun meta(action: ItemMeta.() -> Unit) {
        MenuEngine.requireSync()
        editMeta(action)
    }

    fun <T : ItemMeta> meta(type: Class<T>, action: T.() -> Unit) = meta {
        require(type.isInstance(this)) { "Материал предмета не поддерживает ${type.simpleName}" }
        type.cast(this).action()
    }

    fun attribute(attribute: Attribute, modifier: AttributeModifier) = meta {
        require(modifier.amount.isFinite()) { "Значение атрибута должно быть конечным" }
        require(addAttributeModifier(attribute, modifier)) { "Модификатор атрибута уже добавлен: ${modifier.key}" }
    }

    fun attribute(
        attribute: Attribute,
        key: NamespacedKey,
        amount: Double,
        operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER,
        slot: EquipmentSlotGroup = EquipmentSlotGroup.ANY
    ) = attribute(attribute, AttributeModifier(key, amount, operation, slot))

    fun enchant(enchantment: Enchantment, level: Int, unsafe: Boolean = false) = meta {
        require(level > 0 && (unsafe || level in enchantment.startLevel..enchantment.maxLevel)) {
            "Недопустимый уровень зачарования: $level"
        }
        if (this is EnchantmentStorageMeta) addStoredEnchant(enchantment, level, unsafe)
        else addEnchant(enchantment, level, unsafe)
    }

    fun potionColor(color: Color?) = meta(PotionMeta::class.java) { setColor(color) }

    fun potionType(type: PotionType?) = meta(PotionMeta::class.java) { basePotionType = type }

    fun potionEffect(effect: PotionEffect, overwrite: Boolean = true) =
        meta(PotionMeta::class.java) { addCustomEffect(effect, overwrite) }

    fun headTexture(value: String) {
        MenuEngine.requireSync()
        val texture = value.trim()
        val url = when {
            texture.matches(Regex("[a-fA-F0-9]{32,64}")) -> "https://textures.minecraft.net/texture/$texture"
            texture.startsWith("https://") || texture.startsWith("http://") -> texture
            else -> try {
                val json = Base64.getDecoder().decode(texture).decodeToString()
                JsonParser.parseString(json).asJsonObject.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN").get("url").asString
            } catch (exception: RuntimeException) {
                throw IllegalArgumentException("Укажите URL, хеш или Base64 текстуры головы", exception)
            }
        }
        val uri = URI.create(url)
        require(uri.scheme in setOf("http", "https") && uri.host.equals("textures.minecraft.net", true) &&
            uri.rawPath.orEmpty().matches(Regex("/texture/[a-fA-F0-9]{32,64}"))) {
            "Ожидается ссылка на текстуру textures.minecraft.net"
        }
        val skin = URI.create("https://textures.minecraft.net" + uri.rawPath)
        val id = UUID.nameUUIDFromBytes(skin.toString().encodeToByteArray())
        meta(SkullMeta::class.java) {
            val profile = Bukkit.createProfileExact(id, null)
            val textures = profile.textures
            textures.skin = skin.toURL()
            profile.setTextures(textures)
            playerProfile = profile
        }
    }

    fun modelData(value: Int) = modelData(value.toFloat())

    fun modelData(value: Float) {
        require(value.isFinite()) { "Значение модели должно быть конечным числом" }
        modelData { floats = listOf(value) }
    }

    fun modelData(configure: CustomModelDataComponent.() -> Unit) = meta {
        setCustomModelDataComponent(customModelDataComponent.apply(configure))
    }

    fun itemModel(model: NamespacedKey?) = meta { itemModel = model }

    fun equippable(slot: EquipmentSlot, configure: EquippableComponent.() -> Unit = {}) = meta {
        val component = equippable
        component.slot = slot
        component.configure()
        setEquippable(component)
    }

    fun <P : Any, T : Any> nbt(key: NamespacedKey, type: PersistentDataType<P, T>, value: T?) = meta {
        if (value == null) persistentDataContainer.remove(key)
        else persistentDataContainer.set(key, type, value)
    }

    fun flags(vararg flags: ItemFlag) = meta { addItemFlags(*flags) }

    fun unbreakable(value: Boolean = true) = meta { isUnbreakable = value }

    fun damage(value: Int) = meta(Damageable::class.java) {
        require(value >= 0) { "Урон предмета не может быть отрицательным" }
        damage = value
    }
}
