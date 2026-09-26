package com.neverreader.app.settings.view.preferences

import android.content.DialogInterface
import android.util.SparseArray
import android.view.View
import com.neverreader.app.App
import com.neverreader.app.settings.AbsPrefsFragment
import com.neverreader.app.settings.view.preferences.MultipleChoicePreference.OnSelectedItemChangedListener
import com.neverreader.util.java.Logs
import com.neverreader.util.prefs.BooleanPreference
import com.neverreader.util.prefs.IntPreference

/**
 * Helper class for easily creating preference views for use in [AbsPrefsFragment].
 */
object PreferenceViews {

    private fun getString(res: Int): String {
        return App.getStringResource(res) ?: ""
    }

    /**
     * Creates a new Preference Header. [Preference]
     *
     * @param frag
     * @param title
     * @return
     */
    fun newHeader(frag: AbsPrefsFragment, title: Int): HeaderPreference {
        return newHeader(frag, getString(title), true)
    }

    /**
     * Creates a new Preference Header. [Preference]
     *
     * @param frag
     * @param title
     * @return
     */
    fun newHeader(frag: AbsPrefsFragment, title: Int, topDivider: Boolean): HeaderPreference {
        return newHeader(frag, getString(title), topDivider)
    }

    /**
     * Creates a new Preference Header. [Preference]
     *
     * @param frag
     * @param title
     * @return
     */
    /**
     * Creates a new Preference Header. [Preference]
     */
    @JvmOverloads
    fun newHeader(
        frag: AbsPrefsFragment,
        title: String,
        topDivider: Boolean = true
    ): HeaderPreference {
        return HeaderPreference(frag, title, topDivider)
    }

    /**
     * Creates a new [ActionBuilder] to create a [ActionPreference].
     */
    fun newActionBuilder(frag: AbsPrefsFragment, title: Int): ActionBuilder {
        return newActionBuilder(frag, getString(title))
    }

    /**
     * Creates a new [ActionBuilder] to create a [ActionPreference].
     */
    fun newActionBuilder(frag: AbsPrefsFragment, title: String): ActionBuilder {
        return ActionBuilder(frag, title)
    }

    /**
     * Creates a new [ImportantBuilder] to create a [ImportantPreference].
     */
    fun newImportantBuilder(frag: AbsPrefsFragment, title: Int): ImportantBuilder {
        return newImportantBuilder(frag, getString(title))
    }

    /**
     * Creates a new [ImportantBuilder] to create a [ImportantPreference].
     */
    fun newImportantBuilder(frag: AbsPrefsFragment, title: String): ImportantBuilder {
        return ImportantBuilder(frag, title)
    }

    /** Creates a new [ToggleSwitchBuilder] to create a [ToggleSwitchPreference].  */
    fun newToggleSwitchBuilder(
        frag: AbsPrefsFragment,
        pref: BooleanPreference,
        title: Int
    ): ToggleSwitchBuilder {
        return newToggleSwitchBuilder(frag, pref, getString(title))
    }

    /** Creates a new [ToggleSwitchBuilder] to create a [ToggleSwitchPreference].  */
    fun newToggleSwitchBuilder(
        frag: AbsPrefsFragment,
        pref: ToggleSwitchPreference.PrefHandler,
        title: Int
    ): ToggleSwitchBuilder {
        return newToggleSwitchBuilder(frag, pref, getString(title))
    }

    /** Creates a new [ToggleSwitchBuilder] to create a [ToggleSwitchPreference].  */
    fun newToggleSwitchBuilder(
        frag: AbsPrefsFragment,
        pref: BooleanPreference,
        title: String?
    ): ToggleSwitchBuilder {
        return newToggleSwitchBuilder(
            frag,
            object : ToggleSwitchPreference.PrefHandler {
                override fun get(): Boolean {
                    return pref.get()
                }

                override fun set(value: Boolean) {
                    pref.set(value)
                }
            },
            title
        )
    }

    /** Creates a new [ToggleSwitchBuilder] to create a [ToggleSwitchPreference].  */
    fun newToggleSwitchBuilder(
        frag: AbsPrefsFragment,
        pref: ToggleSwitchPreference.PrefHandler,
        title: String?
    ): ToggleSwitchBuilder {
        return ToggleSwitchBuilder(frag, title, pref)
    }

    /**
     * Creates a new [MultipleChoiceBuilder] to create a [MultipleChoicePreference].
     *
     * @param frag
     * @param pref The preference that will be controlled by this picker
     * @param title
     * @return
     */
    fun newMultipleChoiceBuilder(
        frag: AbsPrefsFragment,
        pref: IntPreference,
        title: Int
    ): MultipleChoiceBuilder {
        return newMultipleChoiceBuilder(frag, pref, getString(title))
    }

    /**
     * Creates a new [MultipleChoiceBuilder] to create a [MultipleChoicePreference].
     *
     * @param frag
     * @param pref The preference that will be controlled by this picker
     * @param title
     * @return
     */
    fun newMultipleChoiceBuilder(
        frag: AbsPrefsFragment,
        pref: IntPreference,
        title: String?
    ): MultipleChoiceBuilder {
        return MultipleChoiceBuilder(frag, title, pref)
    }

    fun newMultipleChoiceBuilder(
        frag: AbsPrefsFragment,
        title: Int,
        pref: MultipleChoicePreference.PrefHandler?
    ): MultipleChoiceBuilder {
        return newMultipleChoiceBuilder(frag, getString(title), pref)
    }

    fun newMultipleChoiceBuilder(
        frag: AbsPrefsFragment,
        title: String,
        pref: MultipleChoicePreference.PrefHandler?
    ): MultipleChoiceBuilder {
        return MultipleChoiceBuilder(frag, title, pref)
    }



    abstract class SettingBuilder(
        protected val frag: AbsPrefsFragment,
        protected val title: String?
    ) {
        protected val summary: SparseArray<CharSequence?> = SparseArray<CharSequence?>()
        protected var enabledWhen: EnabledCondition? = null
        protected var identifier: String? = null

        /**
         * Set the description text below the label/title for the default and/or unchecked state.
         * @param value
         * @return
         */
        open fun setSummaryDefaultUnchecked(value: Int): SettingBuilder {
            return setSummaryDefaultUnchecked(getString(value))
        }

        /**
         * Set the description text below the label/title for the default and/or unchecked state.
         * @param value
         * @return
         */
        open fun setSummaryDefaultUnchecked(value: String?): SettingBuilder {
            summary.put(ActionPreference.Companion.SUMMARY_DEFAULT_OR_UNCHECKED, value)
            return this
        }

        /**
         * Set the description text to be used when the preference is disabled.
         * @param value
         * @return
         */
        open fun setSummaryDisabled(value: Int): SettingBuilder {
            summary.put(ActionPreference.Companion.SUMMARY_UNAVAILABLE, getString(value))
            return this
        }

        /**
         * If invoked, the preference will appear disabled unless the referenced preference equals the value.
         * @param pref The preference to check
         * @param value The value the referenced preference must be for this preference to be shown as enabled
         * @return
         */
        open fun setEnabledWhen(pref: BooleanPreference, value: Boolean): SettingBuilder {
            enabledWhen = object : EnabledCondition {
                override val isTrue: Boolean
                    get() = pref.get() == value
            }
            return this
        }

        /**
         * If invoked, the preference will appear disabled unless the condition evaluates to true.
         */
        open fun setEnabledWhen(enabledWhen: EnabledCondition?): SettingBuilder {
            this.enabledWhen = enabledWhen
            return this
        }

        fun setIdentifier(identifier: String?): SettingBuilder {
            this.identifier = identifier
            return this
        }

        abstract fun build(): Preference?
    }

    class ImportantBuilder(frag: AbsPrefsFragment, title: String) :
        ActionBuilder(frag, title) {
        public override fun build(): ImportantPreference {
            return ImportantPreference(
                frag,
                title!!,
                onClickListener,
                onLongClickListener,
                enabledWhen,
                identifier
            )
        }
    }

    open class ActionBuilder(frag: AbsPrefsFragment, title: String?) :
        SettingBuilder(frag, title) {
        protected var onClickListener: ActionPreference.OnClickAction? = null
        protected var onLongClickListener: ActionPreference.OnClickAction? = null

        override fun setSummaryDefaultUnchecked(value: Int): ActionBuilder {
            return super.setSummaryDefaultUnchecked(value) as ActionBuilder
        }

        override fun setSummaryDefaultUnchecked(value: String?): ActionBuilder {
            return super.setSummaryDefaultUnchecked(value) as ActionBuilder
        }

        override fun setSummaryDisabled(value: Int): ActionBuilder {
            return super.setSummaryDisabled(value) as ActionBuilder
        }

        override fun setEnabledWhen(pref: BooleanPreference, value: Boolean): ActionBuilder {
            return super.setEnabledWhen(pref, value) as ActionBuilder
        }

        override fun setEnabledWhen(enabledWhen: EnabledCondition?): ActionBuilder {
            return super.setEnabledWhen(enabledWhen) as ActionBuilder
        }

        /**
         * Set an action to be performed when clicked.
         * @param listener
         * @return
         */
        open fun setOnClickListener(listener: ActionPreference.OnClickAction?): ActionBuilder {
            this.onClickListener = listener
            return this
        }

        /**
         * Set an action to be performed when long pressed. This is not for normal use,
         * mostly for hidden features that the support team can tell people about as needed.
         *
         * @param listener
         * @return
         */
        open fun setOnLongClickListener(listener: ActionPreference.OnClickAction?): ActionBuilder {
            this.onLongClickListener = listener
            return this
        }

        override fun build(): ActionPreference {
            return ActionPreference(
                frag,
                title,
                if (summary.size() > 0) summary else null,
                onClickListener, onLongClickListener,
                enabledWhen, identifier
            )
        }
    }

    class ToggleSwitchBuilder(
        frag: AbsPrefsFragment,
        title: String?,
        private val pref: ToggleSwitchPreference.PrefHandler
    ) : SettingBuilder(frag, title) {
        private var onChangeListener: ToggleSwitchPreference.OnChangeListener? = null

        override fun setSummaryDefaultUnchecked(value: Int): ToggleSwitchBuilder {
            return super.setSummaryDefaultUnchecked(value) as ToggleSwitchBuilder
        }

        override fun setSummaryDefaultUnchecked(value: String?): ToggleSwitchBuilder {
            return super.setSummaryDefaultUnchecked(value) as ToggleSwitchBuilder
        }

        override fun setSummaryDisabled(value: Int): ToggleSwitchBuilder {
            return super.setSummaryDisabled(value) as ToggleSwitchBuilder
        }

        override fun setEnabledWhen(pref: BooleanPreference, value: Boolean): ToggleSwitchBuilder {
            return super.setEnabledWhen(pref, value) as ToggleSwitchBuilder
        }

        override fun setEnabledWhen(enabledWhen: EnabledCondition?): ToggleSwitchBuilder {
            this.enabledWhen = enabledWhen
            return this
        }

        /**
         * Set the description text to be used when the preference is checked.
         * @param value
         * @return
         */
        fun setSummaryChecked(value: Int): ToggleSwitchBuilder {
            return setSummaryChecked(getString(value))
        }

        /**
         * Set the description text to be used when the preference is checked.
         * @param value
         * @return
         */
        fun setSummaryChecked(value: String?): ToggleSwitchBuilder {
            summary.put(ActionPreference.SUMMARY_CHECKED, value)
            return this
        }

        /**
         * Set a listener to be invoked before and after the user toggles the checkbox
         * @param listener
         * @return
         */
        fun setOnChangeListener(listener: ToggleSwitchPreference.OnChangeListener?): ToggleSwitchBuilder {
            this.onChangeListener = listener
            return this
        }

        /**
         * A simpler, lambda supported variant if you want onChange to just return true.
         */
        fun setOnChangeListener(listener: SimpleOnChangeListener): ToggleSwitchBuilder {
            return setOnChangeListener(object : ToggleSwitchPreference.OnChangeListener {
                override fun onChange(view: View?, nowEnabled: Boolean): Boolean {
                    return true
                }

                override fun afterChange(nowEnabled: Boolean) {
                    listener.afterChange(nowEnabled)
                }
            })
        }

        interface SimpleOnChangeListener {
            fun afterChange(nowEnabled: Boolean)
        }

        override fun build(): ToggleSwitchPreference {
            return ToggleSwitchPreference(
                frag,
                pref,
                title!!,
                if (summary.size() > 0) summary else null,
                onChangeListener,
                enabledWhen,
                identifier
            )
        }
    }

    class MultipleChoiceBuilder(
        frag: AbsPrefsFragment,
        title: String?,
        private val pref: MultipleChoicePreference.PrefHandler?
    ) : ActionBuilder(frag, title) {
        private var onItemSelectedListener: OnSelectedItemChangedListener? = null

        constructor(frag: AbsPrefsFragment, title: String?, pref: IntPreference) : this(
            frag,
            title,
            object : MultipleChoicePreference.PrefHandler {
                var mSelected: Int = 0
                override fun getSelected(): Int {
                    return mSelected
                }

                override fun setSelected(index: Int) {
                    mSelected = index
                }
            })

        /** Not allowed for [MultipleChoiceBuilder]. Use [.addChoice]  */
        public override fun setSummaryDefaultUnchecked(value: Int): MultipleChoiceBuilder {
            Logs.throwIfNotProduction("not allowed on this pref type, use addChoice instead")
            return this
        }

        /** Not allowed for [MultipleChoiceBuilder]. Use [.addChoice]  */
        public override fun setSummaryDefaultUnchecked(value: String?): MultipleChoiceBuilder {
            Logs.throwIfNotProduction("not allowed on this pref type, use addChoice instead")
            return this
        }

        /** Not allowed for [MultipleChoiceBuilder]. Use [.addChoice]  */
        public override fun setSummaryDisabled(value: Int): MultipleChoiceBuilder {
            Logs.throwIfNotProduction("not allowed on this pref type, use addChoice instead")
            return this
        }

        /** REVIEW i don't believe this pref type supports being disabled?  */
        public override fun setEnabledWhen(
            pref: BooleanPreference,
            value: Boolean
        ): MultipleChoiceBuilder {
            return super.setEnabledWhen(pref, value) as MultipleChoiceBuilder
        }

        /** Not allowed for [MultipleChoiceBuilder].  */
        override fun setOnClickListener(listener: ActionPreference.OnClickAction?): ActionBuilder {
            Logs.throwIfNotProduction("setOnClickListener not allowed for checkboxes, use on changed listeners instead.")
            return super.setOnClickListener(listener)
        }

        /** Not allowed for [MultipleChoiceBuilder].  */
        override fun setOnLongClickListener(listener: ActionPreference.OnClickAction?): ActionBuilder {
            Logs.throwIfNotProduction("setOnLongClickListener not allowed for checkboxes, use on changed listeners instead.")
            return super.setOnClickListener(listener)
        }

        /**
         * Set a listener to be invoked when the user picks a new value
         * @param listener
         * @return
         */
        fun setOnItemSelectedListener(listener: OnSelectedItemChangedListener?): MultipleChoiceBuilder {
            this.onItemSelectedListener = listener
            return this
        }

        /**
         * A simpler, lambda supported variant if you want onItemSelected to just return true.
         */
        fun setOnItemSelectedListener(listener: SimpleOnSelectedItemChangedListener): MultipleChoiceBuilder {
            return setOnItemSelectedListener(object : OnSelectedItemChangedListener {
                override fun onItemSelected(
                    view: View?,
                    newValue: Int,
                    dialog: DialogInterface?
                ): Boolean {
                    return true
                }

                override fun onItemSelectionChanged(newValue: Int) {
                    listener.onItemSelectionChanged(newValue)
                }
            })
        }

        interface SimpleOnSelectedItemChangedListener {
            fun onItemSelectionChanged(newValue: Int)
        }

        /**
         * Invoke in order, for as many options as should be available in the picker dialog. These are the labels the user
         * will pick from and will also be displayed under the label/title of the preference view to show the currently selected option.
         *
         * @param label
         * @return
         */
        fun addChoice(label: Int): MultipleChoiceBuilder {
            return addChoice(getString(label))
        }

        /**
         * Invoke in order, for as many options as should be available in the picker dialog. These are the labels the user
         * will pick from and will also be displayed under the label/title of the preference view to show the currently selected option.
         *
         * @param label
         * @return
         */
        fun addChoice(label: String?): MultipleChoiceBuilder {
            summary.put(summary.size(), label)
            return this
        }

        @Suppress("deprecation")
        public override fun build(): MultipleChoicePreference {
            return MultipleChoicePreference(
                frag,
                pref,
                title!!,
                summary,
                onItemSelectedListener,
                enabledWhen,
                identifier
            )
        }
    }

    interface EnabledCondition {
        val isTrue: Boolean
    }
}
