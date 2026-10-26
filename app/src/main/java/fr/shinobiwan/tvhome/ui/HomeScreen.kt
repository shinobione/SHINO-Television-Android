@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package fr.shinobiwan.tvhome.ui

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.*
import fr.shinobiwan.tvhome.HomeState
import fr.shinobiwan.tvhome.HomeViewModel
import fr.shinobiwan.tvhome.R
import fr.shinobiwan.tvhome.core.*
import fr.shinobiwan.tvhome.data.TvApp
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val Ink=Color(0xFF0B0B10)
val Panel=Color(0xFF19181F)
val Muted=Color(0xFFA8A3B6)
val Violet=Color(0xFFA477FF)
val Mint=Color(0xFF51D7AF)
private val Paris=ZoneId.of("Europe/Paris")
private val clockFormat=DateTimeFormatter.ofPattern("HH:mm").withZone(Paris)
private val dateFormat=DateTimeFormatter.ofPattern("EEEE d MMMM",Locale.FRENCH).withZone(Paris)
private val resetFormat=DateTimeFormatter.ofPattern("EEE d MMM · HH:mm",Locale.FRENCH).withZone(Paris)

@Composable fun HomeScreen(state:HomeState,model:HomeViewModel){
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var resumeEpoch by remember { mutableIntStateOf(0) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle){
        val observer=LifecycleEventObserver { _, event -> if(event==Lifecycle.Event.ON_RESUME)resumeEpoch++ }
        lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}
    }
    val visible=remember(state.apps,state.settings.order,state.settings.hidden){
        state.apps.filter { it.packageName !in state.settings.hidden }.sortedWith(compareBy<TvApp>{
            val i=state.settings.order.indexOf(it.packageName); if(i<0)Int.MAX_VALUE else i
        }.thenBy { it.label })
    }
    val requesters=remember(visible.map{it.packageName}){visible.associate{it.packageName to FocusRequester()}}
    val settingsFocus=remember{FocusRequester()}
    LaunchedEffect(settingsOpen,resumeEpoch,visible.map{it.packageName}){
        if(!settingsOpen){delay(150);try{ (requesters[state.lastApp]?:requesters.values.firstOrNull()?:settingsFocus).requestFocus() }catch(_:IllegalStateException){} }
    }
    MaterialTheme(colorScheme=darkColorScheme(primary=Violet,onPrimary=Color.White,surface=Panel,onSurface=Color.White,background=Ink)){
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF17101F),Ink,Color(0xFF111019))))){
            if(settingsOpen)SettingsScreen(state,model,onClose={settingsOpen=false})
            else Column(Modifier.fillMaxSize().padding(horizontal=40.dp,vertical=16.dp)){
                Header(state,Modifier.fillMaxWidth(),{settingsOpen=true},settingsFocus)
                Spacer(Modifier.height(12.dp))
                if(visible.isEmpty()){
                    Column(Modifier.fillMaxWidth().height(110.dp).background(Panel,RoundedCornerShape(14.dp)).padding(20.dp)){
                        Text("Vos applications apparaîtront ici",fontSize=20.sp)
                        Text("Applications TV installées uniquement · vérifier les raccourcis dans Paramètres",color=Muted,fontSize=14.sp)
                    }
                }else LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(8.dp),modifier=Modifier.fillMaxWidth().height(100.dp)){
                    items(visible,key={it.packageName}){app->AppTile(app,requesters.getValue(app.packageName)){model.launch(app)}}
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Eyebrow("LE STUDIO, EN UN REGARD",Modifier.weight(1f))
                    if(state.settings.demo)Text("DEMO · données fictives",color=Color(0xFFE5B64D),fontSize=13.sp)
                    else Text(connectionLabel(state.online,state.snapshot?.generatedAt,state.now),color=if(state.online)Mint else Muted,fontSize=13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp)){
                    state.settings.widgets.take(3).forEach{id->
                        WidgetRegistry.definitions[id]?.let { def ->
                            WidgetShell(def.title,Modifier.weight(if(id=="codex")1.2f else 1f).fillMaxHeight()){def.content(state,model)}
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text("SHINO // TV HOME  v0.1",color=Muted,fontSize=11.sp,modifier=Modifier.weight(1f))
                    Text(state.message.ifBlank{"↑ ↓ ← → Naviguer     OK Ouvrir     Retour Quitter"},color=Muted,fontSize=11.sp,maxLines=1)
                }
            }
        }
    }
}
@Composable private fun Header(s:HomeState,modifier:Modifier,onSettings:()->Unit,settingsFocus:FocusRequester){
    Row(modifier.height(72.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){
            Image(painterResource(R.drawable.brand),"Logo SHINOBIWAN officiel",Modifier.width(180.dp).height(56.dp),contentScale=ContentScale.Fit)
            Text("T V   H O M E",fontSize=10.sp,color=Violet,letterSpacing=4.sp,modifier=Modifier.padding(start=3.dp))
        }
        Column(horizontalAlignment=Alignment.End){
            Text(clockFormat.format(s.now),fontSize=40.sp,fontWeight=FontWeight.Light,lineHeight=42.sp)
            Text(dateFormat.format(s.now).replaceFirstChar{it.titlecase(Locale.FRENCH)},fontSize=14.sp,color=Muted)
        }
        Column(Modifier.width(180.dp).border(1.dp,Color(0xFF39313F),RoundedCornerShape(12.dp)).padding(12.dp)){
            WeatherText(s,compact=true)
        }
        Action("Paramètres",onSettings,Modifier.focusRequester(settingsFocus))
    }
}
// Separate helper keeps the header's horizontal arrangement explicit.
@Composable fun Eyebrow(text:String,modifier:Modifier=Modifier){Text(text,modifier,color=Muted,fontSize=11.sp,fontWeight=FontWeight.Medium,letterSpacing=2.sp)}
@Composable fun Action(text:String,onClick:()->Unit,modifier:Modifier=Modifier){
    Button(onClick=onClick,modifier=modifier.height(32.dp),colors=ButtonDefaults.colors(containerColor=Color(0xFF292530),contentColor=Color.White,focusedContainerColor=Violet,focusedContentColor=Color.White),
        scale=ButtonDefaults.scale(focusedScale=1.04f),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)){
        Text(text,fontSize=13.sp)
    }
}
@Composable private fun AppTile(app:TvApp,requester:FocusRequester,onClick:()->Unit){
    var focused by remember{mutableStateOf(false)}
    Button(onClick=onClick,modifier=Modifier.width(150.dp).height(82.dp).focusRequester(requester).onFocusChanged{focused=it.isFocused},
        shape=ButtonDefaults.shape(shape=RoundedCornerShape(14.dp)),
        colors=ButtonDefaults.colors(containerColor=Panel,focusedContainerColor=Color(0xFF33263E),focusedContentColor=Color.White),
        border=ButtonDefaults.border(focusedBorder=Border(border=BorderStroke(2.dp,Violet),shape=RoundedCornerShape(14.dp))),
        scale=ButtonDefaults.scale(focusedScale=1.05f),contentPadding=PaddingValues(10.dp)){
        Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
            app.icon?.let{Image(it.asImageBitmap(),null,Modifier.size(32.dp),contentScale=ContentScale.Fit)}
                ?:Text(app.label.take(1),fontSize=30.sp,color=Violet)
            Spacer(Modifier.height(6.dp))
            Text(app.label,fontSize=14.sp,fontWeight=if(focused)FontWeight.Bold else FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}
@Composable private fun WidgetShell(title:String,modifier:Modifier,content:@Composable ColumnScope.()->Unit){
    Column(modifier.background(Panel,RoundedCornerShape(16.dp)).border(1.dp,Color(0xFF302B38),RoundedCornerShape(16.dp)).padding(14.dp)){
        Text(title,color=Muted,fontSize=10.sp,letterSpacing=1.5.sp,fontWeight=FontWeight.Medium)
        Spacer(Modifier.height(8.dp));content()
    }
}
data class WidgetDefinition(val title:String,val content:@Composable (HomeState,HomeViewModel)->Unit)
object WidgetRegistry {
    val definitions=linkedMapOf(
        "codex" to WidgetDefinition("CODEX PERISCOPE"){s,_->CodexWidget(s)},
        "media" to WidgetDefinition("NOW PLAYING"){s,m->MediaWidget(s,m)},
        "clock" to WidgetDefinition("ICI & MAINTENANT"){s,_->ClockWidget(s)},
        "telemetry" to WidgetDefinition("SHINO // LINK"){s,_->TelemetryWidget(s)}
    )
}
@Composable private fun CodexWidget(s:HomeState){
    val demo=s.settings.demo
    val now=s.now
    val five=if(demo)Quota(83.0,null)else s.snapshot?.five
    val week=if(demo)Quota(62.0,null)else s.snapshot?.week
    QuotaRow("5 heures",five, demo || s.online && s.snapshot?.status=="AVAILABLE" && five?.isCurrent(s.snapshot.capturedAt,now)==true)
    Spacer(Modifier.height(6.dp))
    QuotaRow("Semaine",week, demo || s.online && s.snapshot?.status=="AVAILABLE" && week?.isCurrent(s.snapshot.capturedAt,now)==true)
    Spacer(Modifier.height(6.dp))
    Text(if(demo)"DEMO · quotas fictifs" else "Quota restant · ${ageLabel(s.snapshot?.capturedAt,now)}",color=Muted,fontSize=10.sp)
    Text("Activité : UNVERIFIED",color=Muted,fontSize=9.sp)
}
@Composable private fun QuotaRow(label:String,quota:Quota?,current:Boolean){
    val color=when(quota?.let{quotaBand(it.remaining)}){QuotaBand.VIOLET->Violet;QuotaBand.TURQUOISE->Mint;QuotaBand.GOLD->Color(0xFFE5B64D);QuotaBand.ORANGE->Color(0xFFF28C38);QuotaBand.RED->Color(0xFFEE4B55);else->Muted}
    Row(verticalAlignment=Alignment.Bottom){
        Text(label,color=Muted,fontSize=12.sp,modifier=Modifier.weight(1f))
        Text(quota?.let{"${it.remaining.toInt()} %"}?:"—",fontSize=24.sp,color=if(current)color else Muted,fontWeight=FontWeight.Medium)
    }
    Meter((quota?.remaining?:0.0).toFloat()/100,if(current)color else Muted)
    Text(if(quota==null)"Non disponible" else if(!current)"Cache · données non actuelles" else quota.resetsAt?.let{"Reset ${resetFormat.format(it)}"}?:"Réinitialisation indisponible",color=Muted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
}
@Composable fun Meter(value:Float,color:Color){
    Box(Modifier.fillMaxWidth().padding(vertical=5.dp).height(4.dp).background(Color(0xFF39333F),RoundedCornerShape(3.dp))){
        Box(Modifier.fillMaxHeight().fillMaxWidth(value.coerceIn(0f,1f)).background(color,RoundedCornerShape(3.dp)))
    }
}
@Composable private fun MediaWidget(s:HomeState,model:HomeViewModel){
    val media=if(s.settings.demo)Media("AVAILABLE","DEMO","Morceau de démonstration","Artiste fictif",playing=true,positionMs=42000,durationMs=180000) else model.preferredMedia(s)
    if(media.status!="AVAILABLE"){
        Text("Le calme, pour l'instant.",fontSize=20.sp,fontWeight=FontWeight.Light)
        Spacer(Modifier.height(10.dp))
        Text(if(media.status=="EMPTY")"Aucune lecture détectée · ${media.source}" else "Source ${s.settings.mediaSource} indisponible",fontSize=12.sp,color=Muted)
        Spacer(Modifier.height(8.dp))
        Text("Média TV facultatif dans Paramètres. La source PC exige une passerelle compatible.",fontSize=11.sp,color=Muted)
    }else{
        Row(verticalAlignment=Alignment.CenterVertically){
            if(media.artwork!=null)Image(media.artwork.asImageBitmap(),"Pochette",Modifier.size(56.dp),contentScale=ContentScale.Crop)
            else Box(Modifier.size(56.dp).background(Color(0xFF2C2538),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Text("♫",fontSize=30.sp,color=Violet)}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)){
                Text(media.title.ifBlank{"Titre indisponible"},fontSize=17.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(media.artist.ifBlank{"Artiste indisponible"},fontSize=12.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("${media.source} · ${if(media.playing)"Lecture" else "Pause"}",fontSize=10.sp,color=Mint)
            }
        }
        Spacer(Modifier.height(12.dp))
        if(media.durationMs>0){Meter(media.positionMs.toFloat()/media.durationMs,Violet);Text("${duration(media.positionMs)} / ${duration(media.durationMs)} · position observée",fontSize=10.sp,color=Muted)}
        else Text("Progression indisponible",fontSize=10.sp,color=Muted)
        Text(media.app,fontSize=10.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            if(media.source=="TV"&&media.canPlayPause)Action(if(media.playing)"Pause" else "Lecture",model::toggleMedia)
            if(media.source=="TV"&&media.canNext)Action("Suivant",model::nextMedia)
        }
    }
}
private fun duration(ms:Long)="${ms/60000}:${(ms/1000%60).toString().padStart(2,'0')}"
@Composable private fun ClockWidget(s:HomeState){
    Text(clockFormat.format(s.now),fontSize=36.sp,fontWeight=FontWeight.Light)
    Text("Europe / Paris",fontSize=11.sp,color=Muted)
    Spacer(Modifier.height(8.dp));WeatherText(s,false)
}
@Composable private fun WeatherText(s:HomeState,compact:Boolean){
    val weather=s.weather?.takeIf{it.city==s.settings.city}
    if(weather==null){Text("Météo indisponible",fontSize=if(compact)14.sp else 16.sp);Text(s.settings.city,fontSize=11.sp,color=Muted,maxLines=1)}
    else{
        Text("${weather.celsius.toInt()}°  ${weatherCondition(weather.code)}",fontSize=if(compact)17.sp else 20.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        Text(weather.city,fontSize=11.sp,color=Muted,maxLines=1)
        val stale=!s.weatherOnline || weather.observedAt<s.now.minusSeconds(3600)
        Text("${if(stale)"Cache · " else ""}${ageLabel(weather.observedAt,s.now)}",fontSize=10.sp,color=Muted)
    }
    if(!compact)Text("Météo : Open-Meteo · CC BY 4.0",fontSize=9.sp,color=Muted)
}
private fun weatherCondition(code:Int)=when(code){0->"☀ Ciel clair";1,2,3->"☁ Nuages";45,48->"Brume";in 51..67->"Pluie";in 71..77->"Neige";in 80..82->"Averses";in 85..86->"Neige";in 95..99->"Orage";else->"Conditions inconnues"}
@Composable private fun TelemetryWidget(s:HomeState){
    val data=s.snapshot?.telemetry
    val current=s.online && s.snapshot?.generatedAt?.let{it>s.now.minusSeconds(120)}==true
    Text(if(current)"PC connecté" else "PC hors ligne",fontSize=20.sp,color=if(current)Mint else Muted)
    Spacer(Modifier.height(12.dp))
    if(data==null)Text("Activer la lecture SHINO LINK dans la passerelle. Aucune mesure inventée.",fontSize=12.sp,color=Muted)
    else{
        Text("CPU ${data.cpu?.toInt()?.toString()?:"—"} %   GPU ${data.gpu?.toInt()?.toString()?:"—"} %",fontSize=16.sp)
        Text("RAM ${data.ramGb?.let{String.format(Locale.FRANCE,"%.1f",it)}?:"—"} Go",fontSize=20.sp)
        Text("GPU ${data.gpuC?.toInt()?.toString()?:"—"} °C · ${if(current)"source LINK" else "cache"}",fontSize=11.sp,color=Muted)
    }
}
