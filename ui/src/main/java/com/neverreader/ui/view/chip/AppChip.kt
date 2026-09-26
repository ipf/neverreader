package com.neverreader.ui.view.chip

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.databinding.BindingAdapter
import com.neverreader.ui.analytics.UiEntityable
import com.neverreader.ui.R
import com.neverreader.ui.databinding.ViewChipBinding
import com.neverreader.ui.util.toPxInt
import com.neverreader.ui.view.themed.ThemedLinearLayout

@Suppress("MagicNumber")
class AppChip(
    context: Context,
    attrs: AttributeSet?,
) : ThemedLinearLayout(
    context,
    attrs,
    entityType = UiEntityable.Type.BUTTON,
) {

    private val binding: ViewChipBinding = ViewChipBinding.inflate(
        LayoutInflater.from(context),
        this,
    ).also {
        // setting params here because our root layout in xml is a <merge> tag
        orientation = HORIZONTAL
        background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
        updatePadding(
            left = 12f.toPxInt(context),
            right = 12f.toPxInt(context),
            top = 6f.toPxInt(context),
            bottom = 8f.toPxInt(context),
        )
    }

    private var isSelectable: Boolean = true
    private var hasImage: Boolean = false
    private var selectedImage: Int = 0
    private var unselectedImage: Int = 0
    var badgeVisible: Boolean = false
        set(value) {
            binding.badge.visibility = if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }
            field = value
        }
    var closeVisible: Boolean = false
        set(value) {
            binding.close.visibility = if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }
            field = value
        }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NeverReaderChip,
            0,
            0
        ).apply {
            binding.text.text = getString(R.styleable.NeverReaderChip_chipText)
            selectedImage = getResourceId(R.styleable.NeverReaderChip_chipIcon, 0)
            unselectedImage = getResourceId(R.styleable.NeverReaderChip_chipIconUnselected, 0)
            if (selectedImage != 0) {
                setImage(isSelected)
                hasImage = true
            } else {
                binding.icon.visibility = View.GONE
            }
            isSelectable = getBoolean(R.styleable.NeverReaderChip_chipSelectable, true)
        }
        if (binding.text.text.isBlank() || !hasImage) {
            binding.spaceBetweenIconAndText.visibility = View.GONE
        }

        badgeVisible = false
        closeVisible = false
    }

    override fun setSelected(selected: Boolean) {
        if (isSelectable) {
            super.setSelected(selected)
            setImage(selected)
        }
    }

    private fun setImage(selected: Boolean) {
        val image = getImage(selected)
        if (image != 0) {
            binding.icon.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    image
                )
            )
        }
    }

    private fun getImage(selected: Boolean): Int =
        if (!selected && unselectedImage != 0) {
            unselectedImage
        } else {
            selectedImage
        }

    companion object {
        @BindingAdapter("chipSelected")
        fun isSelected(view: AppChip, selected: Boolean) {
            view.isSelected = selected
        }

        @BindingAdapter("badgeVisible")
        fun setBadgeVisibility(view: AppChip, badgeVisible: Boolean) {
            view.badgeVisible = badgeVisible
        }

        @BindingAdapter("closeVisible")
        fun setCloseVisibility(view: AppChip, closeVisible: Boolean) {
            view.closeVisible = closeVisible
        }

        @BindingAdapter("chipText")
        fun setChipText(view: AppChip, text: String) {
            view.binding.text.text = text
            if (view.hasImage) {
                view.binding.spaceBetweenIconAndText.visibility = View.VISIBLE
            }
        }

        @BindingAdapter("enabled")
        fun setEnabled(view: AppChip, enabled: Boolean) {
            view.isEnabled = enabled
            view.binding.text.isEnabled = enabled
            view.binding.icon.isEnabled = enabled
            view.binding.close.isEnabled = enabled
        }
    }
}