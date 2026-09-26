package com.neverreader.app.settings

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neverreader.app.R
import com.neverreader.app.settings.view.preferences.Preference
import com.neverreader.app.settings.view.preferences.Preference.PrefViewType
import com.neverreader.app.settings.view.preferences.Preference.SimplePreference
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.sdk.util.dialog.AlertMessaging
import com.neverreader.ui.view.AppBar
import com.neverreader.ui.view.empty.LoadableLayout
import com.neverreader.ui.view.menu.SectionHeaderView
import com.neverreader.ui.view.settings.SettingsImportantButton
import com.neverreader.ui.view.settings.SettingsSwitchView
import com.neverreader.util.java.UserFacingErrorMessage
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import org.apache.commons.lang3.StringUtils

@AndroidEntryPoint
abstract class AbsPrefsFragment : AbsNeverReaderFragment() {
    protected var layout: ViewGroup? = null
    protected var list: RecyclerView? = null

    protected var prefAdapter: PrefAdapter? = null

    protected var appbar: AppBar? = null
    protected var loading: LoadableLayout? = null

    private val mPrefs = ArrayList<Preference>()
    private var listener: Disposable? = null

    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater!!.inflate(R.layout.activity_settings, container, false)
    }

    public override fun onViewCreatedImpl(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedImpl(view, savedInstanceState)

        layout = findViewById<ViewGroup?>(R.id.rootView)

        appbar = findViewById<AppBar?>(R.id.appbar)
        appbar!!.bind().title(if (this.title != 0) getString(this.title) else "").onLeftIconClick(
            View.OnClickListener { v: View? -> finish() })

        loading = findViewById<LoadableLayout?>(R.id.loading)
        loading!!.bind().clear()

        prefAdapter = PrefAdapter()

        list = findViewById<RecyclerView?>(R.id.list)
        list!!.setLayoutManager(LinearLayoutManager(getContext()))
        list!!.setAdapter(prefAdapter)

        createPrefs(mPrefs)
    }



    public override fun onDestroyView() {
        super.onDestroyView()
        prefAdapter = null
        layout = null
        list = null
        appbar = null
        loading = null
    }

    protected fun showProgress() {
        loading!!.bind().showProgressIndeterminate()
        list!!.setVisibility(View.GONE)
    }

    protected fun hideProgress() {
        list!!.setVisibility(View.VISIBLE)
    }

    protected fun showError(e: Throwable?, retry: View.OnClickListener?) {
        if (isDetachedOrFinishing) return
        val msg = StringUtils.defaultIfBlank<String?>(
            UserFacingErrorMessage.find(e),
            getString(R.string.dg_api_generic_error)
        )
        loading!!.bind().showEmptyOrError()!!.clear()
            .title(getResources().getText(R.string.dg_error_t))
            .message(msg)
            .errorButton(getResources().getText(R.string.ac_retry))
            .buttonOnClick(retry)
            .buttonOnLongClick(OnLongClickListener { v: View? ->
                true
            })
    }

    protected abstract val title: Int

    protected abstract fun createPrefs(prefs: ArrayList<Preference>?)

    protected abstract val bannerView: View?

    protected fun rebuildPrefs() {
        if (isDetachedOrFinishing) {
            return
        }
        mPrefs.clear()
        createPrefs(mPrefs)
        prefAdapter!!.notifyDataSetChanged()
    }

    inner class PrefAdapter() :
        RecyclerView.Adapter<PrefAdapter.ViewHolder>() {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long {
            // allows for predictive animations and not messing with the toggle switch animation,
            // even though we're calling notifyDatasetChanged all the time.
            return mPrefs.get(position).hashCode().toLong()
        }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            fun bind(pref: Preference) {
                val itemView = this.itemView
                if (pref.isClickable) {
                    itemView.setOnClickListener(pref)
                    itemView.setOnLongClickListener(pref)
                    itemView.setClickable(true)
                } else {
                    itemView.setOnClickListener(null)
                    itemView.setOnLongClickListener(null)
                    itemView.setClickable(false)
                }
                itemView.setEnabled(pref.isEnabled)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val type: PrefViewType? = PrefViewType.entries.getOrNull(viewType)
            val v: View? = when (type) {
                PrefViewType.BANNER -> bannerView
                PrefViewType.HEADER -> SectionHeaderView(context!!)
                PrefViewType.ACTION, PrefViewType.TOGGLE -> SettingsSwitchView(context!!)
                PrefViewType.IMPORTANT -> SettingsImportantButton(context!!)
                else -> SettingsSwitchView(context!!)
            }
            v!!.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pref = mPrefs.get(position)

            // just display the banner
            if (pref.type == PrefViewType.BANNER) {
                return
            }

            pref.applyToView(holder.itemView)
            holder.bind(pref)
        }

        override fun getItemCount(): Int {
            return mPrefs.size
        }

        override fun getItemViewType(position: Int): Int {
            return mPrefs[position].type?.ordinal ?: 0
        }

        fun remove(pref: Preference?) {
            val index = mPrefs.indexOf(pref)
            mPrefs.remove(pref)
            prefAdapter!!.notifyItemRemoved(index)
        }
    }

    fun onPreferenceChange(forceViewUpdate: Boolean) {
        var changed = forceViewUpdate
        for (pref in mPrefs) {
            if (pref.update()) {
                changed = true
            }
        }
        if (changed) {
            prefAdapter!!.notifyDataSetChanged()
        }
    }

    fun setHeaderVisibility(visible: Boolean) {
        if (bannerView == null) {
            return
        }
        if (visible && (mPrefs.isEmpty() || mPrefs.get(0).type != PrefViewType.BANNER)) {
            mPrefs.add(0, object : SimplePreference(this) {
                public override val type: PrefViewType
        get() {
                    return PrefViewType.BANNER
                }
            })
            prefAdapter!!.notifyItemInserted(0)
        } else if (!visible && mPrefs.isNotEmpty() && mPrefs.get(0).type == PrefViewType.BANNER) {
            mPrefs.removeAt(0)
            prefAdapter!!.notifyItemRemoved(0)
        }
    }
}

