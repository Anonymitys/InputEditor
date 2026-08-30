@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.ekko.editor

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCObjectBase
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGLineCap
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSAttributedString
import platform.Foundation.NSCoder
import platform.Foundation.NSMakeRange
import platform.Foundation.NSMutableAttributedString
import platform.Foundation.NSRange
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSString
import platform.Foundation.appendAttributedString
import platform.Foundation.attributedSubstringFromRange
import platform.Foundation.create
import platform.Foundation.decodeObjectForKey
import platform.Foundation.deleteCharactersInRange
import platform.Foundation.encodeObject
import platform.Foundation.enumerateAttributesInRange
import platform.Foundation.length
import platform.Foundation.replaceCharactersInRange
import platform.Foundation.setAttributedString
import platform.UIKit.NSAttachmentAttributeName
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.NSLayoutManager
import platform.UIKit.NSTextAttachment
import platform.UIKit.NSTextContainer
import platform.UIKit.NSTextStorage
import platform.UIKit.UIBezierPath
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeSystem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIFont
import platform.UIKit.UIFontMetrics
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateCancelled
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIGestureRecognizerStateFailed
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageRenderingMode
import platform.UIKit.UILabel
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIMenuController
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextGranularity
import platform.UIKit.UITextStorageDirectionForward
import platform.UIKit.UITextDragDelegateProtocol
import platform.UIKit.UITextDragRequestProtocol
import platform.UIKit.UITextDraggableProtocol
import platform.UIKit.UITextItemInteraction
import platform.UIKit.UITextView
import platform.UIKit.UITextViewDelegateProtocol
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode
import platform.UIKit.contentEdgeInsets
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.math.abs
import kotlin.math.max

/**
 * 与 Android 端 DoubaoInputBar / ChipSpan / view_editor_input_bar.xml 对齐的视觉度量。
 *
 * 单位全部是 pt（iOS 1pt 对应 Android 1dp）；
 * 字号用 [UIFontMetrics] 缩放，对应 Android 的 sp（跟随系统字体缩放）。
 */
internal object IosBarMetrics {
    /** 输入正文，对应 Android 的 15sp。 */
    val bodyFont: UIFont =
        UIFontMetrics.defaultMetrics().scaledFontForFont(UIFont.systemFontOfSize(15.0))

    /** chip 内文字，对应 Android 的 14sp。 */
    val chipLabelFont: UIFont =
        UIFontMetrics.defaultMetrics().scaledFontForFont(UIFont.systemFontOfSize(14.0))

    /**
     * 对应 Android EditText 的 paddingTop / paddingBottom = 10dp；
     * iOS 上额外加大到 16，给选中状态的光标手柄和上下小球留出绘制余量。
     */
    const val VerticalInset = 16.0

    /** 对应 Android EditText 的 paddingStart = 10dp / paddingEnd = 6dp。 */
    const val TextLeftInset = 10.0
    const val TextRightInset = 6.0

    /** 对应 Android ImageButton 的 40dp。 */
    const val SendButtonSize = 40.0

    /** 对应 Android ImageButton 的 padding = 11dp，图标实绘 18pt。 */
    const val SendIconInset = 11.0
    const val SendIconSize = 18.0

    /** chip 几何，与 ChipSpan 一致：高 26、圆角取半。 */
    const val ChipHeight = 26.0
    const val ChipPaddingStart = 8.0
    const val ChipIconSize = 16.0
    const val ChipIconTextGap = 6.0
    const val ChipTextCloseGap = 4.0
    const val ChipCloseSize = 16.0
    const val ChipPaddingEnd = 6.0

    /** chip 文字最多显示的字符数（按码点计），对应 ChipSpan.MAX_LABEL_CHARS。 */
    const val ChipMaxLabelChars = 5

    /** 空输入栏的初始高度：一行正文 + 上下内边距。 */
    val initialBarHeight: Double
        get() = bodyFont.lineHeight + VerticalInset * 2

    // 颜色与 Android 资源一一对应
    val TextColor = UIColor(red = 26.0 / 255.0, green = 26.0 / 255.0, blue = 26.0 / 255.0, alpha = 1.0)
    val HintColor = UIColor(red = 153.0 / 255.0, green = 153.0 / 255.0, blue = 153.0 / 255.0, alpha = 1.0)
    val SendBgEnabled = UIColor(red = 77.0 / 255.0, green = 107.0 / 255.0, blue = 254.0 / 255.0, alpha = 1.0)
    val SendBgDisabled = UIColor(red = 226.0 / 255.0, green = 226.0 / 255.0, blue = 226.0 / 255.0, alpha = 1.0)
    val SendIconDisabled = UIColor(red = 160.0 / 255.0, green = 160.0 / 255.0, blue = 160.0 / 255.0, alpha = 1.0)
    val ChipBackground = UIColor(red = 232.0 / 255.0, green = 236.0 / 255.0, blue = 255.0 / 255.0, alpha = 1.0)
    val ChipForeground = SendBgEnabled

    /** 关闭按钮圆底 = 前景色 18% 透明，对应 ChipSpan 里的 closeCirclePaint。 */
    val ChipCloseCircle = ChipForeground.colorWithAlphaComponent(0.18)
}

/**
 * 输入框里的富文本块：占一个 ￼ 字符位，与 Android 端 ChipSpan 对应。
 *
 * 内联进 NSAttributedString。整块不可拆分，放不下时整块换行，与 Android 一致。
 *
 * 通过 NSCoding 归档 chipLabel / chipIcon，供同进程内的 copy/paste 复用；
 * chipData 无法序列化，解码后置空，删除回调由输入栏重新接线。
 */
private class ChipAttachment : NSTextAttachment {
    val chipLabel: String
    val chipData: Any?
    val chipIcon: ChipIconType?

    constructor(label: String, data: Any?, icon: ChipIconType?) : super(data = null, ofType = null) {
        chipLabel = label
        chipData = data
        chipIcon = icon
        render()
    }

    @ObjCObjectBase.OverrideInit
    constructor(coder: NSCoder) : super(coder) {
        chipLabel = coder.decodeObjectForKey("chipLabel") as? String ?: ""
        chipData = null
        chipIcon = (coder.decodeObjectForKey("chipIcon") as? String)?.let { name ->
            ChipIconType.entries.firstOrNull { it.name == name }
        }
        if (chipLabel.isNotEmpty()) {
            render()
        }
    }

    private fun render() {
        val width = measureChipWidth(chipLabel, chipIcon)
        image = renderChipImage(chipLabel, chipIcon, width)
        // 以基线为中心（offset = -height / 2），与 Android ChipSpan 画在基线上一致
        bounds = CGRectMake(
            0.0,
            -IosBarMetrics.ChipHeight / 2.0,
            width,
            IosBarMetrics.ChipHeight,
        )
    }

    override fun encodeWithCoder(coder: NSCoder) {
        super.encodeWithCoder(coder)
        coder.encodeObject(chipLabel, forKey = "chipLabel")
        chipIcon?.let { coder.encodeObject(it.name, forKey = "chipIcon") }
    }
}

/**
 * 输入栏容器（UIKit 原生实现），与 Android 端 DoubaoInputBar 行为一致：
 * - 单行起步，随内容长高，到达 maxLines 后内部滚动；
 * - chip 以附件形式内联，点右上角关闭按钮整块删除；
 * - 有文字时发送按钮可用。
 *
 * 高度上报统一放在 [layoutSubviews]：先给输入框正确宽度、再量 contentSize，
 * 保证首帧、旋转、行数限制变化后高度都正确。
 *
 * TextKit 1：显式搭建 NSTextStorage -> NSLayoutManager -> NSTextContainer 栈，
 * 通过 UITextView(frame:textContainer:) 指定使用 TextKit 1，不依赖 iOS 16 的
 * TextKit 2 默认行为。
 */
internal class IosNativeInputBar : UIView, UIGestureRecognizerDelegateProtocol {

    @ObjCObjectBase.OverrideInit
    constructor() : super(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))

    @ObjCObjectBase.OverrideInit
    constructor(coder: NSCoder) : super(coder)

    /** 与 Android DoubaoInputBar 的回调一一对应。 */
    var onSend: (String) -> Unit = {}
    var onChipRemoved: (Any?) -> Unit = {}
    var onTextChanged: (String) -> Unit = {}

    /** 期望高度（pt）变化时回调，由 Compose 侧驱动外层 frame。 */
    var onHeightChanged: (Double) -> Unit = {}

    var placeholder: String = ""
        set(value) {
            field = value
            placeholderLabel.text = value
        }

    var minLines: Int = 1
        set(value) {
            val clamped = value.coerceAtLeast(1)
            if (field != clamped) {
                field = clamped
                setNeedsLayout()
            }
        }

    var maxLines: Int = 8
        set(value) {
            val clamped = value.coerceAtLeast(minLines)
            if (field != clamped) {
                field = clamped
                setNeedsLayout()
            }
        }

    private val textView: UITextView

    private val placeholderLabel = UILabel(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
        font = IosBarMetrics.bodyFont
        textColor = IosBarMetrics.HintColor
        numberOfLines = 1
    }

    private val sendButton = UIButton.buttonWithType(UIButtonTypeSystem).apply {
        setImage(renderSendIcon(), forState = UIControlStateNormal)
        imageView?.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        contentEdgeInsets = UIEdgeInsetsMake(
            IosBarMetrics.SendIconInset,
            IosBarMetrics.SendIconInset,
            IosBarMetrics.SendIconInset,
            IosBarMetrics.SendIconInset,
        )
        layer.cornerRadius = IosBarMetrics.SendButtonSize / 2.0
        layer.masksToBounds = true
        enabled = false
        backgroundColor = IosBarMetrics.SendBgDisabled
        tintColor = IosBarMetrics.SendIconDisabled
        addTarget(
            this@IosNativeInputBar,
            action = NSSelectorFromString("handleSendTapped"),
            forControlEvents = UIControlEventTouchUpInside,
        )
    }

    private val textDelegate = IosBarTextViewDelegate(this)

    /** 返回空 drag items，从源头禁止文本拖拽与 lift 放大。 */
    private val chipDragDelegate = ChipTextDragDelegate()

    /** 点击 chip 关闭按钮的手势：只负责关闭按钮命中，命中失败时放行给系统。 */
    private val chipTapGesture = UITapGestureRecognizer(
        target = this,
        action = NSSelectorFromString("handleChipTap:"),
    ).apply {
        cancelsTouchesInView = false
        delegate = this@IosNativeInputBar
    }

    /**
     * 长按手势，取代系统长按选词：
     * - 长按 chip 附件：吞掉，什么都不做（不放大、不选中、不弹菜单）；
     * - 长按普通文本：选中长按位置的词，弹编辑菜单。
     */
    private val chipLongPressGesture = UILongPressGestureRecognizer(
        target = this,
        action = NSSelectorFromString("handleChipLongPress:"),
    ).apply {
        minimumPressDuration = 0.5
        // 默认只有 10pt，按住时手指轻微抖动会让手势直接 Failed、被当成点击；
        // 放宽到 30pt，保证长按能稳定进入 Began。
        allowableMovement = 30.0
        cancelsTouchesInView = true
        delegate = this@IosNativeInputBar
    }

    private var lastReportedHeight = -1.0

    /** 长按手势是否开始于 chip 附件上。 */
    private var longPressOnChip = false

    init {
        val storage = NSTextStorage()
        val layout = NSLayoutManager()
        val container = NSTextContainer(size = CGSizeMake(0.0, 0.0)).apply {
            widthTracksTextView = true
            // 内边距统一由 textContainerInset 表达，关闭系统默认的每行 5pt 缩进
            lineFragmentPadding = 0.0
        }
        storage.addLayoutManager(layout)
        layout.addTextContainer(container)

        textView = UITextView(
            frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
            textContainer = container,
        ).apply {
            font = IosBarMetrics.bodyFont
            textColor = IosBarMetrics.TextColor
            // 与 Android EditText 的 padding（10/10/10/6dp）一致
            textContainerInset = UIEdgeInsetsMake(
                IosBarMetrics.VerticalInset,
                IosBarMetrics.TextLeftInset,
                IosBarMetrics.VerticalInset,
                IosBarMetrics.TextRightInset,
            )
            // 禁用系统文本拖拽：长按附件不再触发 lift 放大动画。
            // 注意这里用 this 而不是 disableTextDragging()：此时 textView 属性还没赋值，
            // 访问属性会读到 null。
            (this as UITextDraggableProtocol).apply {
                textDragDelegate = chipDragDelegate
                textDragInteraction?.enabled = false
            }
            scrollEnabled = true
            showsVerticalScrollIndicator = true
            backgroundColor = UIColor.clearColor
            typingAttributes = mapOf(
                NSFontAttributeName to IosBarMetrics.bodyFont,
                NSForegroundColorAttributeName to IosBarMetrics.TextColor,
            )
            // 对应 Android 的 textCapSentences
            autocapitalizationType = UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences
            delegate = textDelegate
            addGestureRecognizer(chipTapGesture)
            addGestureRecognizer(chipLongPressGesture)
        }

        addSubview(textView)
        addSubview(placeholderLabel)
        addSubview(sendButton)

        configureTextSelectionGestures()
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val width = bounds.useContents { size.width }
        // 首次布局前宽度还没定，此时量的 contentSize 没有意义，等下一次布局
        if (width <= 0.0) return

        val textWidth = max(0.0, width - IosBarMetrics.SendButtonSize)
        val lineHeight = IosBarMetrics.bodyFont.lineHeight
        val minHeight = lineHeight * minLines + IosBarMetrics.VerticalInset * 2
        val maxHeight =
            max(lineHeight, IosBarMetrics.ChipHeight) * maxLines + IosBarMetrics.VerticalInset * 2

        // 先定宽度再量高度：contentSize 只依赖宽度，与当前 frame 高度无关，
        // 因此首帧（frame 还是 0）和旋转后这里都能算出正确值。
        textView.setFrame(
            CGRectMake(0.0, 0.0, textWidth, textView.frame.useContents { size.height }),
        )
        val contentHeight = textView.contentSize.useContents { height }
        val textHeight = contentHeight.coerceIn(minHeight, maxHeight)
        textView.setFrame(CGRectMake(0.0, 0.0, textWidth, textHeight))

        placeholderLabel.setFrame(
            CGRectMake(
                IosBarMetrics.TextLeftInset,
                IosBarMetrics.VerticalInset,
                max(0.0, textWidth - IosBarMetrics.TextLeftInset - IosBarMetrics.TextRightInset),
                lineHeight,
            ),
        )
        // 发送按钮贴底，与 Android 的 layout_gravity="bottom" 一致
        sendButton.setFrame(
            CGRectMake(
                width - IosBarMetrics.SendButtonSize,
                textHeight - IosBarMetrics.SendButtonSize,
                IosBarMetrics.SendButtonSize,
                IosBarMetrics.SendButtonSize,
            ),
        )

        if (abs(textHeight - lastReportedHeight) > 0.5) {
            lastReportedHeight = textHeight
            val height = textHeight
            // 挪到下一轮 runloop，避免在 Compose 布局过程中同步写状态
            dispatch_async(dispatch_get_main_queue()) {
                onHeightChanged(height)
            }
        }
    }

    /** 与 Android DoubaoInputBar.setText 一致：整体替换、光标移到末尾。 */
    fun setPlainText(value: String) {
        textView.text = value
        textView.typingAttributes = mapOf(
            NSFontAttributeName to IosBarMetrics.bodyFont,
            NSForegroundColorAttributeName to IosBarMetrics.TextColor,
        )
        textView.selectedRange = NSMakeRange(value.length.toULong(), 0uL)
        notifyTextChanged()
    }

    /** 在光标处插入 chip；有选区时先替换选区。与 Android ChipEditText.insertChip 一致。 */
    fun insertChip(label: String, data: Any?, icon: ChipIconType?) {
        val current = NSMutableAttributedString()
        current.setAttributedString(textView.attributedText)
        val selection = textView.selectedRange
        val caret = selection.useContents { location }
        val selectionLength = selection.useContents { length }

        val builder = NSMutableAttributedString()
        // 附件字符和两边空格都带正文字体/颜色：光标停在 chip 旁时，
        // typingAttributes 取自前一个字符，没有字体属性的话后续输入
        // 会回落到 NSLayoutManager 默认的 12pt 小字
        val bodyAttributes: Map<Any?, Any?> = mapOf(
            NSFontAttributeName to IosBarMetrics.bodyFont,
            NSForegroundColorAttributeName to IosBarMetrics.TextColor,
        )
        if (caret > 0uL) {
            val previous = textView.attributedText.string.getOrNull((caret - 1uL).toInt())
            if (previous != ' ' && previous != '\n') {
                builder.appendAttributedString(
                    NSAttributedString.Companion.create(string = " ", attributes = bodyAttributes),
                )
            }
        }

        val attachment = ChipAttachment(label, data, icon)

        builder.appendAttributedString(
            NSAttributedString.Companion.create(
                string = "\uFFFC",
                attributes = bodyAttributes + (NSAttachmentAttributeName to attachment),
            ),
        )
        builder.appendAttributedString(
            NSAttributedString.Companion.create(string = " ", attributes = bodyAttributes),
        )

        current.replaceCharactersInRange(
            NSMakeRange(caret, selectionLength),
            withAttributedString = builder,
        )
        textView.attributedText = current
        textView.typingAttributes = bodyAttributes
        textView.selectedRange = NSMakeRange(caret + builder.length, 0uL)
        textView.becomeFirstResponder()
        notifyTextChanged()
    }

    /**
     * 删除指定的 chip：只删 ￼ 这一个字符位，两边空格保留，与 Android 一致。
     *
     * [charIndex] 是 chip 在文本中的字符下标，由关闭按钮命中时提前算好。
     */
    private fun removeChip(chip: ChipAttachment, charIndex: ULong) {
        val current = NSMutableAttributedString()
        current.setAttributedString(textView.attributedText)
        if (charIndex >= current.length) return

        current.deleteCharactersInRange(NSMakeRange(charIndex, 1uL))
        textView.attributedText = current
        textView.selectedRange = NSMakeRange(charIndex, 0uL)
        notifyTextChanged()
        onChipRemoved(chip.chipData)
    }

    /** 可读文本：chip 展开成 `[label]`，与 Android readableText 一致。 */
    fun readableText(): String {
        val attributed = textView.attributedText
        val builder = StringBuilder()
        attributed.enumerateAttributesInRange(
            NSMakeRange(0uL, attributed.length),
            options = 0uL,
        ) { attrs, range, _ ->
            val attachment = attrs?.get(NSAttachmentAttributeName) as? ChipAttachment
            val chunk = attributed.attributedSubstringFromRange(range).string
            builder.append(if (attachment != null) "[${attachment.chipLabel}]" else chunk)
        }
        return builder.toString()
    }

    fun requestInputFocus() {
        textView.becomeFirstResponder()
    }

    /**
     * 进入编辑状态时兜底禁用文本拖拽。
     *
     * UITextView 的 textDragInteraction 是懒创建的：init 里可能还是 nil，
     * 长按附件会触发 UIDragInteraction 的 lift 动画（附件被放大拎起）。
     * 在每次开始编辑时再禁用一次，保证长按 chip 不做 lift 放大。
     */
    internal fun onEditingBegan() {
        disableTextDragging()
        configureTextSelectionGestures()
    }

    /**
     * 选区变化回调：选区被收起（长度归零，即光标落点）时隐藏编辑菜单。
     * 点击移动光标时，系统会先把选区收成光标，再走到我们自己的点击处理，
     * 所以不能只在点击处理里判断“还有没有选中”，这里统一兜底收菜单。
     */
    internal fun onSelectionChanged() {
        if (textView.selectedRange.useContents { length } == 0uL) {
            UIMenuController.sharedMenuController().setMenuVisible(false, animated = true)
        }
    }

    /**
     * 彻底禁用文本拖拽及 lift 放大：
     * - textDragDelegate 返回空 items，系统文档说明「返回空数组则不会发生拖拽」，
     *   同时也没有可供 lift 放大的 drag item，从源头消除长按放大；
     * - 再兜底把 textDragInteraction.enabled 置 false，避免懒创建时没接上。
     */
    private fun disableTextDragging() {
        val draggable = textView as UITextDraggableProtocol
        draggable.textDragDelegate = chipDragDelegate
        draggable.textDragInteraction?.enabled = false
    }

    /**
     * 配置长按选词手势：
     *
     * 1. 禁用 UITextView 自带的全部长按手势，避免系统长按触发放大镜，
     *    只保留我们自己加的 [chipLongPressGesture]；
     * 2. 让所有点击手势必须等自定义长按失败后才能识别。
     *
     * UITextView 的交互手势可能在进入编辑状态时才懒创建，所以在
     * `textViewDidBeginEditing` 里再兜底调用一次。
     */
    internal fun configureTextSelectionGestures() {
        val longPress = chipLongPressGesture
        textView.gestureRecognizers.orEmpty().forEach { recognizer ->
            when (recognizer) {
                is UILongPressGestureRecognizer -> {
                    if (recognizer != longPress) {
                        recognizer.enabled = false
                    }
                }
                is UITapGestureRecognizer -> {
                    recognizer.requireGestureRecognizerToFail(longPress)
                }
            }
        }
    }

    /** 用户编辑（委托回调入口）：先清理粘贴进来的外来附件，再刷新状态。 */
    internal fun onUserEditedText() {
        stripForeignAttachments()
        notifyTextChanged()
    }

    /** 刷新占位符、发送按钮与文本回调；高度上报由 layoutSubviews 统一负责。 */
    private fun notifyTextChanged() {
        val plain = textView.text
        // hint 的显隐与 Android 一致：text 为空时显示，只有空白也算有内容
        placeholderLabel.hidden = !plain.isNullOrEmpty()
        val hasText = !plain.isNullOrBlank()
        sendButton.enabled = hasText
        sendButton.backgroundColor = if (hasText) IosBarMetrics.SendBgEnabled else IosBarMetrics.SendBgDisabled
        sendButton.tintColor = if (hasText) UIColor.whiteColor else IosBarMetrics.SendIconDisabled
        setNeedsLayout()
        onTextChanged(readableText())
    }

    /**
     * 粘贴富文本可能带进图片等附件；Android 端粘贴只会得到纯文本，
     * 这里把非 chip 的附件整体删掉，保持两端一致。
     */
    private fun stripForeignAttachments() {
        val attributed = textView.attributedText
        val foreignRanges = mutableListOf<CValue<NSRange>>()
        attributed.enumerateAttributesInRange(
            NSMakeRange(0uL, attributed.length),
            options = 0uL,
        ) { attrs, range, _ ->
            val attachment = attrs?.get(NSAttachmentAttributeName)
            if (attachment != null && attachment !is ChipAttachment) {
                foreignRanges.add(range)
            }
        }
        if (foreignRanges.isEmpty()) return

        val current = NSMutableAttributedString()
        current.setAttributedString(attributed)
        for (range in foreignRanges.asReversed()) {
            current.deleteCharactersInRange(range)
        }
        textView.attributedText = current
    }

    @ObjCAction
    fun handleSendTapped() {
        val message = readableText().trim()
        if (message.isNotEmpty()) {
            onSend(message)
            setPlainText("")
            // 对应 Android 发送后 requestFocus()：保持键盘弹起
            textView.becomeFirstResponder()
        }
    }

    /**
     * 点击手势：
     * - 命中 chip 关闭按钮时删除整块（逻辑不变）；
     * - 命中 chip 其它区域时，按左右半区定位光标：左半区光标移到 chip 左侧，右半区移到右侧；
     * - 命中非 chip 区域且有选中时，取消选中。
     */
    @ObjCAction
    fun handleChipTap(sender: UITapGestureRecognizer) {
        if (sender.state != UIGestureRecognizerStateEnded) return
        val point = sender.locationInView(textView)
        val hit = chipAtPoint(point)

        if (hit == null) {
            // 非 chip 区域：取消选中（光标落到点击处），交还给系统定位光标
            clearSelectionAtPoint(point)
            return
        }

        val (chip, charIndex) = hit
        val px = point.useContents { x }
        val py = point.useContents { y }

        val rect = attachmentRectForChipAt(charIndex) ?: return
        if (contains(rect, px, py)) {
            // 关闭按钮优先，命中即删除
            val closeRect = closeButtonRect(rect)
            if (contains(closeRect, px, py)) {
                // 挪到下一轮 runloop，避开系统同一次点击的光标定位
                dispatch_async(dispatch_get_main_queue()) {
                    removeChip(chip, charIndex)
                }
                return
            }

            // 该 chip 已在选中范围内时，点击保持选中（与系统一致），不移动光标
            val selection = textView.selectedRange
            val selStart = selection.useContents { location }
            val selLength = selection.useContents { length }
            val chipSelected = selLength > 0uL &&
                charIndex >= selStart && charIndex < selStart + selLength
            if (chipSelected) return

            // 非关闭按钮：按 chip 中线分左右，光标落到对应一侧
            val centerX = rect.useContents { origin.x } + rect.useContents { size.width } / 2.0
            val cursor = if (px < centerX) charIndex else charIndex + 1uL
            dispatch_async(dispatch_get_main_queue()) {
                textView.selectedRange = NSMakeRange(cursor, 0uL)
            }
        }
    }

    /** 点击非选中区域时取消选中并把光标落到 [point] 处；点在选区内则保留选中。 */
    private fun clearSelectionAtPoint(point: CValue<CGPoint>) {
        val selection = textView.selectedRange
        val start = selection.useContents { location }
        val length = selection.useContents { length }
        if (length == 0uL) return

        val position = textView.closestPositionToPoint(point) ?: return
        val beginning = textView.beginningOfDocument
        val rawOffset = textView.offsetFromPosition(beginning, toPosition = position)
        if (rawOffset < 0L) return
        val offset = rawOffset.toULong()

        // 点击落在选中范围内：与系统一致，保留选中和菜单
        if (offset >= start && offset < start + length) return

        textView.selectedRange = NSMakeRange(offset, 0uL)
        UIMenuController.sharedMenuController().setMenuVisible(false, animated = true)
    }

    /**
     * 长按手势：
     * - 长按 chip 附件：吞掉，什么都不做（不放大、不选中、不弹菜单）；
     * - 长按普通文本：松手（UP）时选中长按位置的词，弹编辑菜单。
     */
    @ObjCAction
    fun handleChipLongPress(sender: UILongPressGestureRecognizer) {
        when (sender.state) {
            UIGestureRecognizerStateBegan -> {
                // 记录长按起点是否落在 chip 上；落在 chip 上则整个手势吞掉
                longPressOnChip = chipAtPoint(sender.locationInView(textView)) != null
            }

            UIGestureRecognizerStateEnded -> {
                if (longPressOnChip) {
                    longPressOnChip = false
                    return
                }
                val point = sender.locationInView(textView)
                val position = textView.closestPositionToPoint(point) ?: return
                val wordRange = textView.tokenizer.rangeEnclosingPosition(
                    position,
                    withGranularity = UITextGranularity.UITextGranularityWord,
                    inDirection = UITextStorageDirectionForward,
                ) ?: return
                textView.selectedTextRange = wordRange
                val menuRect = textView.firstRectForRange(wordRange)
                UIMenuController.sharedMenuController().setTargetRect(menuRect, inView = textView)
                UIMenuController.sharedMenuController().setMenuVisible(true, animated = true)
            }

            UIGestureRecognizerStateCancelled,
            UIGestureRecognizerStateFailed -> {
                longPressOnChip = false
            }

            else -> Unit
        }
    }

    /** 命中检查：返回触摸点落在其上的 (chip, 字符下标)，未命中 chip 区域返回 null。 */
    private fun chipAtPoint(point: CValue<CGPoint>): Pair<ChipAttachment, ULong>? {
        val position = textView.closestPositionToPoint(point) ?: return null
        val beginning = textView.beginningOfDocument
        val offset = textView.offsetFromPosition(beginning, toPosition = position)
        val length = textView.attributedText.length.toLong()

        // 触摸点可能落在 chip 之后，因此同时检查 offset 和 offset-1 两处
        val candidates = buildList {
            if (offset in 0 until length) add(offset.toULong())
            if (offset - 1L in 0 until length) add((offset - 1L).toULong())
        }
        val px = point.useContents { x }
        val py = point.useContents { y }

        for (charIndex in candidates) {
            val chip = chipAttachmentAt(charIndex) ?: continue
            val rect = attachmentRectForChipAt(charIndex) ?: continue
            if (contains(rect, px, py)) {
                return chip to charIndex
            }
        }
        return null
    }

    /** [charIndex] 处的字符是否携带 chip 附件。 */
    private fun chipAttachmentAt(charIndex: ULong): ChipAttachment? {
        val attributed = textView.attributedText
        if (charIndex >= attributed.length) return null
        val attrs = attributed.attributesAtIndex(charIndex, effectiveRange = null)
        return attrs[NSAttachmentAttributeName] as? ChipAttachment
    }

    /** 计算 chip 附件占用的布局矩形（textView 坐标），与 firstRectForRange 一致。 */
    private fun attachmentRectForChipAt(charIndex: ULong): CValue<CGRect>? {
        val beginning = textView.beginningOfDocument
        val startPosition = textView.positionFromPosition(beginning, offset = charIndex.toLong()) ?: return null
        val endPosition = textView.positionFromPosition(startPosition, offset = 1L) ?: return null
        val range = textView.textRangeFromPosition(startPosition, toPosition = endPosition) ?: return null
        return textView.firstRectForRange(range)
    }

    /** 计算 chip 关闭按钮的可点击区域（比视觉区域略大），与 Android closeTouchRect 一致。 */
    private fun closeButtonRect(attachmentRect: CValue<CGRect>): CValue<CGRect> {
        val rx = attachmentRect.useContents { origin.x }
        val ry = attachmentRect.useContents { origin.y }
        val rw = attachmentRect.useContents { size.width }
        val rh = attachmentRect.useContents { size.height }

        val closeCenterX = rx + rw - IosBarMetrics.ChipPaddingEnd - IosBarMetrics.ChipCloseSize / 2.0
        val closeCenterY = ry + rh / 2.0
        val touchHalf = IosBarMetrics.ChipCloseSize * 0.7
        return CGRectMake(
            closeCenterX - touchHalf,
            closeCenterY - touchHalf,
            touchHalf * 2.0,
            touchHalf * 2.0,
        )
    }

    /** 点是否落在矩形内（含边界）。 */
    private fun contains(rect: CValue<CGRect>, x: Double, y: Double): Boolean {
        val rx = rect.useContents { origin.x }
        val ry = rect.useContents { origin.y }
        val rw = rect.useContents { size.width }
        val rh = rect.useContents { size.height }
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh
    }

    /** 允许点击手势与 UITextView 自带手势同时识别，否则光标定位会被抢走。 */
    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWithGestureRecognizer: UIGestureRecognizer,
    ): Boolean = true

    /** UIView 与 UIGestureRecognizerDelegateProtocol 都有默认实现，显式走 UIView 的行为。 */
    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean =
        super<UIView>.gestureRecognizerShouldBegin(gestureRecognizer)
}

private class IosBarTextViewDelegate(
    private val bar: IosNativeInputBar,
) : NSObject(), UITextViewDelegateProtocol {

    override fun textViewDidChangeSelection(textView: UITextView) {
        bar.onSelectionChanged()
    }

    override fun textViewDidBeginEditing(textView: UITextView) {
        bar.onEditingBegan()
    }

    override fun textViewDidChange(textView: UITextView) {
        bar.onUserEditedText()
    }

    override fun textView(
        textView: UITextView,
        shouldInteractWithTextAttachment: NSTextAttachment,
        inRange: CValue<NSRange>,
        interaction: UITextItemInteraction,
    ): Boolean {
        // chip 不走系统图片交互：返回 false 交还给文本选择流程
        return shouldInteractWithTextAttachment !is ChipAttachment
    }
}

/** 返回空 drag items，禁用文本拖拽与长按 lift 放大。 */
private class ChipTextDragDelegate : NSObject(), UITextDragDelegateProtocol {
    override fun textDraggableView(
        textDraggableView: UIView,
        itemsForDrag: UITextDragRequestProtocol,
    ): List<*> = emptyList<Any?>()
}

/** chip 标签按码点截断，超出 5 个码点补省略号；emoji 代理对不会被截断。 */
private fun truncateChipLabel(label: String): String {
    var codePointCount = 0
    var index = 0
    while (index < label.length) {
        if (codePointCount == IosBarMetrics.ChipMaxLabelChars) {
            return label.substring(0, index) + "…"
        }
        codePointCount++
        index += if (label[index].isHighSurrogate()) 2 else 1
    }
    return label
}

/** 计算 chip 排版宽度，与 Android ChipSpan.computeWidth 一致。 */
private fun measureChipWidth(label: String, icon: ChipIconType?): Double {
    val displayLabel = truncateChipLabel(label)
    val attributes: Map<Any?, Any?> = mapOf(
        NSFontAttributeName to IosBarMetrics.chipLabelFont,
        NSForegroundColorAttributeName to IosBarMetrics.ChipForeground,
    )
    val labelWidth = NSString.Companion.create(string = displayLabel)
        .sizeWithAttributes(attributes)
        .useContents { width }
    val iconBlock = if (icon != null) {
        IosBarMetrics.ChipIconSize + IosBarMetrics.ChipIconTextGap
    } else {
        0.0
    }
    return IosBarMetrics.ChipPaddingStart + iconBlock + labelWidth +
        IosBarMetrics.ChipTextCloseGap + IosBarMetrics.ChipCloseSize + IosBarMetrics.ChipPaddingEnd
}

/** 把整个 chip 渲染成一张图片（圆角底 + 图标 + 文字 + 关闭按钮）。 */
private fun renderChipImage(label: String, icon: ChipIconType?, width: Double): UIImage {
    val height = IosBarMetrics.ChipHeight
    val displayLabel = truncateChipLabel(label)
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(width, height))
    return renderer.imageWithActions {
        UIBezierPath.bezierPathWithRoundedRect(
            CGRectMake(0.0, 0.0, width, height),
            cornerRadius = height / 2.0,
        ).apply {
            IosBarMetrics.ChipBackground.setFill()
            fill()
        }

        val iconBlock = if (icon != null) {
            IosBarMetrics.ChipIconSize + IosBarMetrics.ChipIconTextGap
        } else {
            0.0
        }

        if (icon != null) {
            buildChipDocumentPath(IosBarMetrics.ChipIconSize / 24.0).apply {
                applyTransform(
                    CGAffineTransformMakeTranslation(
                        IosBarMetrics.ChipPaddingStart,
                        (height - IosBarMetrics.ChipIconSize) / 2.0,
                    ),
                )
                IosBarMetrics.ChipForeground.setFill()
                fill()
            }
        }

        val labelAttributes: Map<Any?, Any?> = mapOf(
            NSFontAttributeName to IosBarMetrics.chipLabelFont,
            NSForegroundColorAttributeName to IosBarMetrics.ChipForeground,
        )
        val textHeight = NSString.Companion.create(string = displayLabel)
            .sizeWithAttributes(labelAttributes)
            .useContents { this.height }
        val labelY = (height - textHeight) / 2.0
        NSString.Companion.create(string = displayLabel).drawAtPoint(
            CGPointMake(IosBarMetrics.ChipPaddingStart + iconBlock, labelY),
            withAttributes = labelAttributes,
        )

        val closeCenterX = width - IosBarMetrics.ChipPaddingEnd - IosBarMetrics.ChipCloseSize / 2.0
        val closeCenterY = height / 2.0
        UIBezierPath.bezierPathWithOvalInRect(
            CGRectMake(
                closeCenterX - IosBarMetrics.ChipCloseSize / 2.0,
                closeCenterY - IosBarMetrics.ChipCloseSize / 2.0,
                IosBarMetrics.ChipCloseSize,
                IosBarMetrics.ChipCloseSize,
            ),
        ).apply {
            IosBarMetrics.ChipCloseCircle.setFill()
            fill()
        }

        val cross = UIBezierPath()
        val arm = IosBarMetrics.ChipCloseSize * 0.42
        cross.moveToPoint(CGPointMake(closeCenterX - arm, closeCenterY - arm))
        cross.addLineToPoint(CGPointMake(closeCenterX + arm, closeCenterY + arm))
        cross.moveToPoint(CGPointMake(closeCenterX - arm, closeCenterY + arm))
        cross.addLineToPoint(CGPointMake(closeCenterX + arm, closeCenterY - arm))
        cross.lineWidth = 1.6
        cross.lineCapStyle = CGLineCap.kCGLineCapRound
        IosBarMetrics.ChipForeground.setStroke()
        cross.stroke()
    }
}

/** 构建文档图标路径（24 视口），按 [scale] 缩放。 */
private fun buildChipDocumentPath(scale: Double): UIBezierPath {
    val path = UIBezierPath()

    // 文档外框 + 折角
    path.moveToPoint(CGPointMake(14.0, 2.0))
    path.addLineToPoint(CGPointMake(6.0, 2.0))
    path.addCurveToPoint(
        CGPointMake(4.01, 4.0),
        CGPointMake(4.9, 2.0),
        CGPointMake(4.01, 2.9),
    )
    path.addLineToPoint(CGPointMake(4.0, 20.0))
    path.addCurveToPoint(
        CGPointMake(5.99, 22.0),
        CGPointMake(4.0, 21.1),
        CGPointMake(4.89, 22.0),
    )
    path.addLineToPoint(CGPointMake(18.0, 22.0))
    path.addCurveToPoint(
        CGPointMake(20.0, 20.0),
        CGPointMake(19.1, 22.0),
        CGPointMake(20.0, 21.1),
    )
    path.addLineToPoint(CGPointMake(20.0, 8.0))
    path.addLineToPoint(CGPointMake(14.0, 2.0))
    path.closePath()

    // 两行文字
    path.moveToPoint(CGPointMake(16.0, 18.0))
    path.addLineToPoint(CGPointMake(8.0, 18.0))
    path.addLineToPoint(CGPointMake(8.0, 16.0))
    path.addLineToPoint(CGPointMake(16.0, 16.0))
    path.closePath()
    path.moveToPoint(CGPointMake(16.0, 14.0))
    path.addLineToPoint(CGPointMake(8.0, 14.0))
    path.addLineToPoint(CGPointMake(8.0, 12.0))
    path.addLineToPoint(CGPointMake(16.0, 12.0))
    path.closePath()

    // 折角
    path.moveToPoint(CGPointMake(13.0, 9.0))
    path.addLineToPoint(CGPointMake(13.0, 3.5))
    path.addLineToPoint(CGPointMake(18.5, 9.0))
    path.closePath()

    path.applyTransform(CGAffineTransformMakeScale(scale, scale))
    return path
}

/** 渲染发送图标：与 Android ic_send.xml 同一路径（Material send，24 视口缩放到 18）。 */
private fun renderSendIcon(): UIImage {
    val size = IosBarMetrics.SendIconSize
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(size, size))
    val image = renderer.imageWithActions {
        val path = UIBezierPath()
        path.moveToPoint(CGPointMake(2.01, 21.0))
        path.addLineToPoint(CGPointMake(23.0, 12.0))
        path.addLineToPoint(CGPointMake(2.01, 3.0))
        path.addLineToPoint(CGPointMake(2.0, 10.0))
        path.addLineToPoint(CGPointMake(17.0, 12.0))
        path.addLineToPoint(CGPointMake(2.0, 14.0))
        path.closePath()
        path.applyTransform(CGAffineTransformMakeScale(size / 24.0, size / 24.0))
        UIColor.blackColor.setFill()
        path.fill()
    }
    // 模板渲染：颜色完全交给 tintColor，对应 Android 的 android:tint
    return image.imageWithRenderingMode(UIImageRenderingMode.UIImageRenderingModeAlwaysTemplate)
}
