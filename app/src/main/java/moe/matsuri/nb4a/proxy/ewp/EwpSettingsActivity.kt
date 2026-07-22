package moe.matsuri.nb4a.proxy.ewp

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.preference.EditTextPreferenceModifiers
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ui.profile.ProfileSettingsActivity
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

class EwpSettingsActivity : ProfileSettingsActivity<EwpBean>() {
    override fun createEntity() = EwpBean().applyDefaultValues()

    private val pbm = PreferenceBindingManager()
    private val name = pbm.add(PreferenceBinding(Type.Text, "name"))
    private val serverAddress = pbm.add(PreferenceBinding(Type.Text, "serverAddress"))
    private val serverPort = pbm.add(PreferenceBinding(Type.TextToInt, "serverPort"))
    private val uuid = pbm.add(PreferenceBinding(Type.Text, "uuid"))
    private val serverStaticPubKey = pbm.add(PreferenceBinding(Type.Text, "serverStaticPubKey"))

    private val type = pbm.add(PreferenceBinding(Type.Text, "type"))
    private val host = pbm.add(PreferenceBinding(Type.Text, "host"))
    private val path = pbm.add(PreferenceBinding(Type.Text, "path"))

    private val xhttpMode = pbm.add(PreferenceBinding(Type.Text, "xhttpMode"))
    private val xhttpPaddingBytes = pbm.add(PreferenceBinding(Type.Text, "xhttpPaddingBytes"))
    private val xhttpXmuxMaxConcurrency = pbm.add(PreferenceBinding(Type.Text, "xhttpXmuxMaxConcurrency"))
    private val xhttpXmuxMaxConnections = pbm.add(PreferenceBinding(Type.Text, "xhttpXmuxMaxConnections"))
    private val xhttpXmuxCMaxReuseTimes = pbm.add(PreferenceBinding(Type.Text, "xhttpXmuxCMaxReuseTimes"))
    private val xhttpXmuxHMaxRequestTimes = pbm.add(PreferenceBinding(Type.Text, "xhttpXmuxHMaxRequestTimes"))
    private val xhttpXmuxHMaxReusableSecs = pbm.add(PreferenceBinding(Type.Text, "xhttpXmuxHMaxReusableSecs"))
    private val xhttpXmuxHKeepAlivePeriod = pbm.add(PreferenceBinding(Type.TextToInt, "xhttpXmuxHKeepAlivePeriod"))

    private val sni = pbm.add(PreferenceBinding(Type.Text, "sni"))
    private val alpn = pbm.add(PreferenceBinding(Type.Text, "alpn"))
    private val certificates = pbm.add(PreferenceBinding(Type.Text, "certificates"))
    private val allowInsecure = pbm.add(PreferenceBinding(Type.Bool, "allowInsecure"))
    private val tlsFragment = pbm.add(PreferenceBinding(Type.Bool, "tlsFragment"))
    private val tlsRecordFragment = pbm.add(PreferenceBinding(Type.Bool, "tlsRecordFragment"))

    private val utlsFingerprint = pbm.add(PreferenceBinding(Type.Text, "utlsFingerprint"))
    private val realityPubKey = pbm.add(PreferenceBinding(Type.Text, "realityPubKey"))
    private val realityShortId = pbm.add(PreferenceBinding(Type.Text, "realityShortId"))

    private val enableECH = pbm.add(PreferenceBinding(Type.Bool, "enableECH"))
    private val echConfig = pbm.add(PreferenceBinding(Type.Text, "echConfig"))
    private val echQueryServerName = pbm.add(PreferenceBinding(Type.Text, "echQueryServerName"))

    private val enableMux = pbm.add(PreferenceBinding(Type.Bool, "enableMux"))
    private val muxPadding = pbm.add(PreferenceBinding(Type.Bool, "muxPadding"))
    private val muxType = pbm.add(PreferenceBinding(Type.TextToInt, "muxType"))
    private val muxConcurrency = pbm.add(PreferenceBinding(Type.TextToInt, "muxConcurrency"))

    override fun EwpBean.init() {
        pbm.writeToCacheAll(this)
    }

    override fun EwpBean.serialize() {
        pbm.fromCacheAll(this)
    }

    override fun PreferenceFragmentCompat.createPreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        addPreferencesFromResource(R.xml.ewp_preferences)

        findPreference<EditTextPreference>(Key.SERVER_PORT)!!.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Port)
        }
        findPreference<EditTextPreference>("uuid")!!.apply {
            summaryProvider = PasswordSummaryProvider
        }

        val xhttpCategory = findPreference<androidx.preference.PreferenceCategory>("serverXhttpCategory")
        val xhttpXmuxCategory = findPreference<androidx.preference.PreferenceCategory>("serverXhttpXmuxCategory")

        fun updateTransportView(network: String) {
            xhttpCategory?.isVisible = network == "xhttp"
            xhttpXmuxCategory?.isVisible = network == "xhttp"
        }

        updateTransportView(type.readStringFromCache())

        findPreference<moe.matsuri.nb4a.ui.SimpleMenuPreference>("type")?.setOnPreferenceChangeListener { _, newValue ->
            updateTransportView(newValue as String)
            true
        }

        // Per-mode default hints: show the library defaults that apply when the
        // corresponding fields are left unset, and refresh them when the mode
        // changes. auto resolves to packet-up on the client (unless REALITY,
        // which becomes stream-one) so treat auto like packet-up here.
        val modeDefaultsInfo = findPreference<androidx.preference.Preference>("xhttpModeDefaultsInfo")
        fun updateXhttpDefaults(mode: String) {
            val isStream = mode == "stream-up" || mode == "stream-one"
            val padding = getString(R.string.xhttp_padding_bytes_default)
            modeDefaultsInfo?.summary = if (isStream) {
                getString(
                    R.string.xhttp_mode_defaults_stream,
                    getString(R.string.xhttp_stream_secs_default),
                    padding,
                )
            } else {
                getString(
                    R.string.xhttp_mode_defaults_packet,
                    getString(R.string.xhttp_max_post_default_packet),
                    getString(R.string.xhttp_post_interval_default_packet),
                    padding,
                )
            }
        }
        updateXhttpDefaults(xhttpMode.readStringFromCache().takeIf { it.isNotBlank() } ?: "auto")
        findPreference<moe.matsuri.nb4a.ui.SimpleMenuPreference>("xhttpMode")
            ?.setOnPreferenceChangeListener { _, newValue ->
                updateXhttpDefaults(newValue as String)
                true
            }

        // REALITY <-> ECH are mutually exclusive (see EwpFmt buildEwpTLS).
        // Hide whichever section is incompatible with the current state
        // so users don't believe both are active.
        val echCategory = findPreference<androidx.preference.Preference>("serverECHCategory")
        val realityPubKeyPref = findPreference<EditTextPreference>("realityPubKey")
        val realityShortIdPref = findPreference<EditTextPreference>("realityShortId")
        val enableEchPref = findPreference<androidx.preference.SwitchPreference>("enableECH")

        fun syncMutex() {
            val realityOn = !realityPubKeyPref?.text.isNullOrBlank()
            val echOn = enableEchPref?.isChecked == true
            echCategory?.isEnabled = !realityOn
            realityPubKeyPref?.isEnabled = !echOn
            realityShortIdPref?.isEnabled = !echOn
        }
        syncMutex()
        realityPubKeyPref?.setOnPreferenceChangeListener { _, _ ->
            view?.post { syncMutex() }; true
        }
        enableEchPref?.setOnPreferenceChangeListener { _, _ ->
            view?.post { syncMutex() }; true
        }
    }
}
