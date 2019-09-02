package io.github.susmithasrimani.gocd.googleChat.chatMessage

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule

@Serializable
data class TextMessage(val text: String)

@Serializable
data class Cards(val cards: List<Card>)

@Serializable
data class Card(val header: Header, val sections: List<Section>)

@Serializable
data class Header(val title: String)

@Serializable
data class Section(val widgets: List<Widget>)

@Serializable
abstract class Widget {
    @Serializer(forClass = Widget::class)
    companion object : KSerializer<Widget> {
        override fun serialize(encoder: Encoder, obj: Widget) {
            when (obj) {
                is ButtonsWidget -> encoder.encode(ButtonsWidget.serializer(), obj)
                is KeyValueWidgetWrapper -> encoder.encode(KeyValueWidgetWrapper.serializer(), obj)
            }
        }
    }
}

@Serializable
data class ButtonsWidget(val buttons: List<Button>) : Widget()

@Serializable
abstract class Button {
    @Serializer(forClass = Button::class)
    companion object : KSerializer<Button> {
        override fun serialize(encoder: Encoder, obj: Button) {
            when (obj) {
                is TextButtonWrapper -> encoder.encode(TextButtonWrapper.serializer(), obj)
            }
        }
    }
}

@Serializable
data class TextButtonWrapper(val textButton: TextButton) : Button()

@Serializable
data class TextButton(val text: String, val onClick: OnClick)

@Serializable
data class OnClick(val openLink: URL)

@Serializable
data class URL(val url: String)

@Serializable
data class KeyValueWidgetWrapper(val keyValue: KeyValue) : Widget()

@Serializable
data class KeyValue(
        val topLabel: String? = null,
        val content: String,
        val onClick: OnClick? = null,
        val button: Button? = null
)
