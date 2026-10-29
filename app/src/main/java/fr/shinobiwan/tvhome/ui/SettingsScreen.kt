@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package fr.shinobiwan.tvhome.ui

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import fr.shinobiwan.tvhome.HomeState
import fr.shinobiwan.tvhome.HomeViewModel
import fr.shinobiwan.tvhome.core.isLanIpv4
import fr.shinobiwan.tvhome.data.parseHubPairingConfig
import fr.shinobiwan.tvhome.data.validReaderToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun SettingsScreen(s:HomeState,model:HomeViewModel,onClose:()->Unit){
    var draft by remember { mutableStateOf(s.settings) }
    var token by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var importInfo by remember { mutableStateOf("") }
    val first=remember { FocusRequester() }
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val importCa=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null)scope.launch {
            try {
                val pem=withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { stream ->
                        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(1024)
                        while(true){val n=stream.read(buffer);if(n<0)break;require(out.size()+n<=16384);out.write(buffer,0,n)}
                        out.toString("UTF-8")
                    }
                }
                require(pem.contains("BEGIN CERTIFICATE") && !pem.contains("PRIVATE KEY"))
                val certs=java.security.cert.CertificateFactory.getInstance("X.509").generateCertificates(pem.byteInputStream())
                require(certs.size in 1..4 && certs.all { it is java.security.cert.X509Certificate && it.basicConstraints>=0 })
                draft=draft.copy(caPem=pem);error="CA publique importée ; enregistrez les réglages."
            } catch(_:Exception){error="Import impossible : choisissez uniquement la CA publique PEM (16 Ko maximum)."}
        }
    }
    val onHubFileSelected: (android.net.Uri?) -> Unit = { uri ->
        if(uri!=null)scope.launch {
            try {
                val config=withContext(Dispatchers.IO) {
                    val bytes=context.contentResolver.openInputStream(uri)?.use { stream ->
                        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(1024)
                        while(true){val n=stream.read(buffer);if(n<0)break;require(out.size()+n<=24576);out.write(buffer,0,n)}
                        out.toByteArray()
                    } ?: throw IllegalArgumentException("Fichier inaccessible")
                    parseHubPairingConfig(String(bytes,Charsets.UTF_8))
                }
                // Do not persist anything until the owner chooses Enregistrer.
                draft=draft.copy(protocol="HUB_V2",host=config.host,port=config.port,
                    caPem=config.caPem,pin=config.certificateSha256,encryptedToken="")
                token=config.readerToken
                error=""
                importInfo="Configuration HUB importée pour ${config.host}:${config.port}. Appuyez sur Enregistrer."
            } catch(e:kotlinx.coroutines.CancellationException){throw e}
            catch(_:Exception) {
                importInfo=""
                error="Import impossible : choisissez un fichier SHINO HUB valide (.json)."
            }
        }
    }
    val importHubDocument=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(),onHubFileSelected)
    val importHubAlternative=rememberLauncherForActivityResult(ActivityResultContracts.GetContent(),onHubFileSelected)
    val apps=s.apps.sortedWith(compareBy{val idx=draft.order.indexOf(it.packageName);if(idx<0)Int.MAX_VALUE else idx})
    BackHandler { onClose() }
    LaunchedEffect(Unit){delay(100);first.requestFocus()}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=40.dp,vertical=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("Votre TV, à votre façon.",fontSize=32.sp)
            Action("Retour",onClose,Modifier.focusRequester(first))
        }
        Text("Application TV standard · Projectivy reste votre accueil de secours",color=Muted,fontSize=17.sp)
        Eyebrow("HORLOGE & MÉTÉO")
        Entry("Ville · Open-Meteo",draft.city,{draft=draft.copy(city=it.take(80))})
        Text("Heure et dates : Europe/Paris · les données météo demandent Internet",color=Muted,fontSize=16.sp)
        Eyebrow("APPAIRAGE PC · HTTPS SUR LE RÉSEAU LOCAL")
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Action("${if(draft.protocol=="HUB_V2")"✓ " else ""}SHINO HUB v2",{draft=draft.copy(protocol="HUB_V2",host="192.168.1.195",port="8767",pin="",encryptedToken="",caPem="");token=""})
            Action("${if(draft.protocol=="V1")"✓ " else ""}Passerelle v1",{draft=draft.copy(protocol="V1",port="8766",pin="",encryptedToken="",caPem="");token=""})
        }
        if(draft.protocol=="HUB_V2") {
            Text("GET /hub/v1/snapshot · identité de lecture TV_HOME · CA + nom IP/SAN + empreinte",color=Muted,fontSize=16.sp)
            Action("Importer configuration HUB depuis une clé USB (.json)",{
                try { importHubDocument.launch(arrayOf("*/*")) }
                catch(_:Exception) {
                    try { importHubAlternative.launch("*/*") }
                    catch(_:Exception){ error="Aucun sélecteur de fichiers disponible sur cette TV." }
                }
            })
            Action("Autre explorateur de fichiers (si USB absent)",{
                try{importHubAlternative.launch("*/*")}
                catch(_:Exception){error="Aucun autre explorateur de fichiers disponible."}
            })
            if(importInfo.isNotBlank())Text(importInfo,color=Color(0xFF79DFBD),fontSize=17.sp)
            Action(if(draft.caPem.isBlank())"Importer la CA publique du HUB (PEM)" else "CA publique importée · remplacer",{try{importCa.launch("*/*")}catch(_:Exception){error="Sélecteur de fichiers indisponible ; utilisez le champ CA ci-dessous."}})
            Entry("CA publique PEM · saisie alternative",draft.caPem,{draft=draft.copy(caPem=it.take(16384))})
        }
        Text("Comparer l'empreinte directement sur le PC. Aucun identifiant OpenAI ne doit être saisi ici.",color=Muted,fontSize=16.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
            Entry("IPv4 privée du PC",draft.host,{draft=draft.copy(host=it.trim().take(15))},Modifier.weight(2f))
            Entry("Port",draft.port,{draft=draft.copy(port=it.filter(Char::isDigit).take(5))},Modifier.weight(1f),numeric=true)
        }
        Entry("Empreinte certificat SHA-256 · 64 caractères hexadécimaux",draft.pin,{draft=draft.copy(pin=it.lowercase().replace(":","").replace(" ","").take(64))})
        Entry(if(draft.encryptedToken.isBlank()) if(draft.protocol=="HUB_V2")"Jeton lecteur TV_HOME · 16 à 128 caractères" else "Secret local d'appairage (32 caractères minimum)" else "Nouveau secret · vide pour conserver l'existant",token,{token=it.take(256)},secret=true)
        Action("Oublier le PC et vider le cache",{
            draft=draft.copy(host="",pin="",encryptedToken="",caPem="");token="";model.save(draft)
        })
        Eyebrow("WIDGETS · UN À TROIS VISIBLES")
        Text("Horloge et météo restent réunies dans le bandeau principal.", fontSize=16.sp, color=Muted)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            WidgetRegistry.definitions.forEach{(id,definition)->
                Action("${if(id in homeWidgetIds(draft.widgets))"✓ " else "+ "}${definition.title}",{
                    if(id in homeWidgetIds(draft.widgets)){if(homeWidgetIds(draft.widgets).size>1)draft=draft.copy(widgets=draft.widgets-id)}
                    else if(homeWidgetIds(draft.widgets).size<3)draft=draft.copy(widgets=homeWidgetIds(draft.widgets)+id)
                    else error="Trois widgets maximum : retirez un widget d'abord."
                })
            }
        }
        Action("Faire tourner l'ordre des widgets",{val ids=homeWidgetIds(draft.widgets);if(ids.size>1)draft=draft.copy(widgets=ids.drop(1)+ids.first())})
        Eyebrow("NOW PLAYING")
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            for(source in listOf("AUTO","TV","PC"))Action("${if(source==draft.mediaSource)"✓ " else ""}$source",{draft=draft.copy(mediaSource=source)})
            Action("Autorisation média TV (facultative)",{
                try{context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}
                catch(_:Exception){error="Réglage indisponible sur cet appareil ; la source PC reste possible."}
            })
        }
        Text("Aucune permission activée automatiquement. Les métadonnées dépendent des applications. La source PC est en lecture seule.",fontSize=16.sp,color=Muted)
        Eyebrow("APPLICATIONS TV INSTALLÉES · ORDRE & VISIBILITÉ")
        if(apps.isEmpty())Text("Aucune application TV détectée sur cet appareil.",color=Muted,fontSize=17.sp)
        apps.forEachIndexed{index,app->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                Text(app.label,modifier=Modifier.weight(1f).padding(vertical=10.dp),fontSize=16.sp)
                Action(if(app.packageName in draft.hidden)"Afficher" else "Masquer",{
                    draft=draft.copy(hidden=if(app.packageName in draft.hidden)draft.hidden-app.packageName else draft.hidden+app.packageName)
                })
                Action("Monter",{
                    if(index>0){val list=apps.map{it.packageName}.toMutableList();java.util.Collections.swap(list,index,index-1);draft=draft.copy(order=list)}
                })
                Action("Descendre",{
                    if(index<apps.lastIndex){val list=apps.map{it.packageName}.toMutableList();java.util.Collections.swap(list,index,index+1);draft=draft.copy(order=list)}
                })
            }
        }
        Eyebrow("OUTILS & DIAGNOSTIC")
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Action(if(draft.demo)"DEMO : activé" else "DEMO : désactivé",{draft=draft.copy(demo=!draft.demo)})
            Action("Paramètres Android",{try{context.startActivity(Intent(AndroidSettings.ACTION_SETTINGS))}catch(_:Exception){error="Paramètres Android indisponibles"}})
            Action("Actualiser le PC",{model.sync()})
        }
        Text("Android ${android.os.Build.VERSION.RELEASE} · API ${android.os.Build.VERSION.SDK_INT} · ${android.os.Build.MODEL}",color=Muted,fontSize=16.sp)
        Text("DEMO affiche des quotas et un morceau fictifs identifiés. Aucun faux raccourci ni donnée matérielle simulée.",color=Muted,fontSize=16.sp)
        if(error.isNotBlank())Text(error,color=Color(0xFFFFAD79),fontSize=17.sp)
        if(s.message.isNotBlank())Text(s.message,color=Muted,fontSize=16.sp)
        Action("Enregistrer et revenir à l'accueil",{
            val pin=draft.pin.replace(":","").replace(" ","")
            error=when{
                draft.city.isBlank()->"Saisissez une ville."
                draft.host.isNotBlank()&&!isLanIpv4(draft.host)->"Utilisez une IPv4 privée (192.168.x.x, 10.x.x.x ou 172.16–31.x.x)."
                draft.host.isNotBlank()&&(draft.port.toIntOrNull()?:0) !in 1024..65535->"Port invalide."
                draft.host.isNotBlank()&&!pin.matches(Regex("[a-f0-9]{64}"))->"L'empreinte SHA-256 doit contenir 64 caractères hexadécimaux."
                draft.host.isNotBlank()&&draft.protocol=="HUB_V2"&&draft.caPem.isBlank()->"Importez la CA publique du HUB."
                token.isNotEmpty()&&draft.protocol=="HUB_V2"&&!validReaderToken(token)->"Jeton TV_HOME : 16 à 128 caractères A-Z, a-z, 0-9, _ ou -."
                token.isNotEmpty()&&draft.protocol=="V1"&&(token.length<32||!token.all{it in 'a'..'z'||it in 'A'..'Z'||it in '0'..'9'})->"Secret local alphanumérique : 32 caractères minimum."
                draft.host.isNotBlank()&&draft.encryptedToken.isBlank()&&token.isBlank()->"Saisissez le secret local d'appairage."
                else->""
            }
            if(error.isBlank()){model.save(draft.copy(city=draft.city.trim(),pin=pin),token.takeIf{it.isNotEmpty()});onClose()}
        })
        Spacer(Modifier.height(20.dp))
    }
}
@Composable private fun Entry(label:String,value:String,onChange:(String)->Unit,modifier:Modifier=Modifier,secret:Boolean=false,numeric:Boolean=false){
    var focused by remember{mutableStateOf(false)}
    Column(modifier){
        Text(label,fontSize=16.sp,color=Muted)
        Spacer(Modifier.height(5.dp))
        BasicTextField(value,onChange,modifier=Modifier.fillMaxWidth().onFocusChanged{focused=it.isFocused}
            .border(if(focused)2.dp else 1.dp,if(focused)Violet else Color(0xFF4D4359),RoundedCornerShape(8.dp)).background(Panel,RoundedCornerShape(8.dp)).padding(12.dp),
            singleLine=true,textStyle=TextStyle(color=Color.White,fontSize=19.sp),cursorBrush=SolidColor(Violet),
            visualTransformation=if(secret)PasswordVisualTransformation()else VisualTransformation.None,
            keyboardOptions=KeyboardOptions(keyboardType=if(secret)KeyboardType.Password else if(numeric)KeyboardType.Number else KeyboardType.Text))
    }
}
