package fr.shinobiwan.tvhome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.shinobiwan.tvhome.core.*
import fr.shinobiwan.tvhome.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant

data class HomeState(val settings:Settings=Settings(),val apps:List<TvApp> = emptyList(),val snapshot:Snapshot?=null,
    val online:Boolean=false,val weather:Weather?=null,val weatherOnline:Boolean=false,val tvMedia:Media=Media(source="TV"),
    val now:Instant=Instant.now(),val message:String="",val lastApp:String?=null)
class HomeViewModel(application:Application):AndroidViewModel(application) {
    val preferences=Preferences(application)
    private val apps=Apps(application)
    private val bridge=BridgeClient(application)
    private val weather=WeatherClient(application)
    private val tv=TvMedia(application)
    private val mutable=MutableStateFlow(HomeState())
    val state=mutable.asStateFlow()
    private var active=false
    private var syncing:Job?=null
    private var weatherJob:Job?=null
    private var loaded=false
    private var blockedPairing: String?=null
    init {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedWeather=weather.cached()
            mutable.update { it.copy(weather=cachedWeather) }
        }
        viewModelScope.launch { preferences.flow.collect { settings ->
            val old=mutable.value.settings
            val changed=loaded && pairingIdentity(old)!=pairingIdentity(settings)
            if(changed) { syncing?.cancelAndJoin(); blockedPairing=null;withContext(Dispatchers.IO){bridge.clearCache()} }
            val initialCache=if(!loaded)withContext(Dispatchers.IO){bridge.cached(settings)}else null
            mutable.update { it.copy(settings=settings,snapshot=if(changed)null else initialCache?:it.snapshot,online=if(changed)false else it.online) }
            loaded=true
            if(active) { sync(); if(old.city!=settings.city || mutable.value.weather==null) refreshWeather() }
        } }
        viewModelScope.launch { tv.flow.collect { media -> mutable.update { it.copy(tvMedia=media) } } }
        viewModelScope.launch { while(isActive) { delay(1000); if(active)mutable.update { it.copy(now=Instant.now()) } } }
        viewModelScope.launch { while(isActive){delay(3000);if(active && mutable.value.settings.protocol=="HUB_V2")sync(false)} }
        viewModelScope.launch { while(isActive){delay(60000);if(active){if(mutable.value.settings.protocol!="HUB_V2")sync(false);refreshWeather()}} }
        viewModelScope.launch { while(isActive){delay(3000);if(active)withContext(Dispatchers.IO){tv.refresh()}} }
    }
    fun resume(){
        active=true
        viewModelScope.launch { val found=apps.installed(); mutable.update { it.copy(apps=found,now=Instant.now()) } }
        viewModelScope.launch(Dispatchers.IO){tv.refresh()}
        if(loaded){sync(false);refreshWeather()}
    }
    fun pause(){ active=false; syncing?.cancel();weatherJob?.cancel();mutable.update { it.copy(online=false) } }
    fun sync(manual: Boolean = true){
        if(syncing?.isActive==true)return
        val settings=mutable.value.settings
        if(!manual && blockedPairing==pairingIdentity(settings))return
        if(settings.demo||settings.host.isBlank()){mutable.update{it.copy(online=false)};return}
        syncing=viewModelScope.launch {
            try {
                val secret=withContext(Dispatchers.IO){preferences.decrypt(settings.encryptedToken)}
                val result=bridge.fetch(settings,secret)
                if(mutable.value.settings.host==settings.host && mutable.value.settings.encryptedToken==settings.encryptedToken)
                    mutable.update { it.copy(snapshot=result,online=true,message="") }
            } catch(e:CancellationException){throw e}
            catch(e:HubFailure){
                if(e.pairingRequired || e.kind=="CONFIGURATION" || e.kind=="TLS")blockedPairing=pairingIdentity(settings)
                mutable.update { it.copy(online=e.kind=="NO_DATA",snapshot=if(e.kind=="NO_DATA")null else it.snapshot,message=e.messageForUser) }
            }
            catch (_:Exception){mutable.update { it.copy(online=false,message="Passerelle inaccessible ou données non valides · cache conservé") }}
        }
    }
    private fun refreshWeather(){
        if(weatherJob?.isActive==true || mutable.value.settings.demo)return
        val city=mutable.value.settings.city
        weatherJob=viewModelScope.launch {
            try { val result=weather.fetch(city); if(mutable.value.settings.city==city) mutable.update{it.copy(weather=result,weatherOnline=true)} }
            catch(e:CancellationException){throw e}
            catch (_:Exception){mutable.update{it.copy(weatherOnline=false)}}
        }
    }
    fun save(settings:Settings, token:String?=null){
        viewModelScope.launch {
            try {
                val secured=if(token!=null) withContext(Dispatchers.IO){settings.copy(encryptedToken=if(token.isBlank()) "" else preferences.encrypt(token))} else settings
                preferences.save(secured)
                mutable.update{it.copy(message="Paramètres enregistrés")}
            }catch (_:Exception){mutable.update{it.copy(message="Enregistrement impossible")}}
        }
    }
    fun launch(app:TvApp){
        mutable.update { it.copy(lastApp = app.packageName) }
        if (!apps.launch(app.packageName)) {
            mutable.update { it.copy(message = "Application indisponible") }
        }
    }
    fun toggleMedia(){tv.toggle()}
    fun nextMedia(){tv.next()}
    fun preferredMedia(s:HomeState):Media {
        val pc=s.snapshot?.media?:Media()
        val meta=s.snapshot?.mediaMeta
        val usablePc=if(meta!=null) s.online && meta.isCurrent(s.now) else s.online && s.snapshot?.generatedAt?.let { it > s.now.minusSeconds(120) && it <= s.now.plusSeconds(120) }==true
        val reliablePc=if(usablePc)pc else if(meta!=null && pc.status=="AVAILABLE")pc.copy(playing=false,quality=meta.label(s.now,s.online)) else Media(source="PC")
        return when(s.settings.mediaSource){
            "TV"->s.tvMedia
            "PC"->reliablePc
            else-> if(s.tvMedia.status=="AVAILABLE"&&s.tvMedia.playing)s.tvMedia
                else if(reliablePc.status=="AVAILABLE"&&reliablePc.playing)reliablePc
                else if(s.tvMedia.status=="AVAILABLE")s.tvMedia else reliablePc
        }
    }
}
