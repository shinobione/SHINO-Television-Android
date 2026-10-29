<#
.SHORTDESCRIPTION
Génère un fichier d'appairage SHINO // TV HOME pour copie sur clé USB.
Lecture uniquement du fichier public HUB et de la CA. Aucun appel réseau.
Le jeton TV_HOME est demandé sur le PC, jamais sur la télécommande.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$PublicConfig,
    [Parameter(Mandatory = $true)][string]$OutputPath
)
$ErrorActionPreference = "Stop"
$cfgPath = [IO.Path]::GetFullPath($PublicConfig)
$cfg = Get-Content -LiteralPath $cfgPath -Raw | ConvertFrom-Json
$hostIp = [string]$cfg.host
$hubPort = [int]$cfg.port
$pin = ([string]$cfg.certificateSha256).Trim().Replace(":", "").ToLowerInvariant()
$octets = $hostIp.Split('.')
$validIpv4 = $octets.Count -eq 4
foreach ($octet in $octets) {
    if ($octet -notmatch '^[0-9]{1,3}$' -or [int]$octet -gt 255 -or
        ($octet.Length -gt 1 -and $octet.StartsWith('0'))) { $validIpv4 = $false; break }
}
$privateIpv4 = $validIpv4 -and (
    [int]$octets[0] -eq 10 -or
    ([int]$octets[0] -eq 192 -and [int]$octets[1] -eq 168) -or
    ([int]$octets[0] -eq 172 -and [int]$octets[1] -ge 16 -and [int]$octets[1] -le 31))
if (-not $privateIpv4 -or $hubPort -lt 1024 -or $hubPort -gt 65535) {
    throw "Adresse HUB ou port public invalide."
}
if ($pin -cnotmatch '^[0-9a-f]{64}$') { throw "Pin SHA256 public invalide." }
$caPath = [string]$cfg.caFile
if (-not [IO.Path]::IsPathRooted($caPath)) {
    $caPath = Join-Path (Split-Path -Parent $cfgPath) $caPath
}
# Avoid PowerShell's provider-specific note properties (PSPath, PSProvider, ...).
# Get-Content -Raw returns a PSObject whose extra properties ConvertTo-Json
# may serialize as an object instead of a PEM string.
$caPem = [IO.File]::ReadAllText([IO.Path]::GetFullPath($caPath), [Text.Encoding]::UTF8)
if (-not $caPem.Contains("BEGIN CERTIFICATE") -or $caPem.Contains("PRIVATE KEY") -or
    [Text.Encoding]::UTF8.GetByteCount($caPem) -gt 16384) {
    throw "La CA publique PEM est invalide ou trop volumineuse."
}
$destination = [IO.Path]::GetFullPath($OutputPath)
if ([IO.Path]::GetExtension($destination).ToLowerInvariant() -ne ".json") {
    throw "La destination doit porter l'extension .json."
}
if (Test-Path -LiteralPath $destination) { throw "Fichier existant : choisir un nouveau nom." }
if (-not (Test-Path -LiteralPath (Split-Path -Parent $destination) -PathType Container)) {
    throw "Le dossier de destination n'existe pas."
}
Write-Host "Collez le jeton de LECTURE TV_HOME sur ce PC (saisie masquée)."
$secure = Read-Host "Jeton TV_HOME" -AsSecureString
$handle = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $readerToken = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($handle)
    if ($readerToken -cnotmatch '^[A-Za-z0-9_-]{16,128}$') {
        throw "Jeton TV_HOME invalide (ne pas utiliser LINK ni PERISCOPE)."
    }
    $document = [ordered]@{
        format = "SHINO_TV_HOME_HUB_PAIRING"
        version = 1
        host = $hostIp
        port = $hubPort
        caPem = $caPem
        certificateSha256 = $pin
        readerToken = $readerToken
    }
    $json = ConvertTo-Json -InputObject $document -Depth 3
    # Verify the actual export contract before writing any credentials to disk.
    $export = ConvertFrom-Json -InputObject $json
    if ($export.caPem -isnot [string] -or
        [string]::IsNullOrWhiteSpace($export.caPem) -or
        [Text.Encoding]::UTF8.GetByteCount($json) -gt 24576) {
        throw "Export invalide : caPem doit etre une chaine PEM, JSON <= 24 Kio."
    }
    [IO.File]::WriteAllText($destination, $json, [Text.UTF8Encoding]::new($false))
    Write-Host "Fichier cree : $destination"
    Write-Host "ATTENTION : le JSON contient le jeton lecteur en clair. Ne jamais le publier sur GitHub."
    Write-Host "Supprimez le fichier de la cle USB apres l'import, si vous n'en avez plus besoin."
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($handle)
    Remove-Variable readerToken, document, json -ErrorAction SilentlyContinue
}
