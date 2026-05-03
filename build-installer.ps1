param(
    [string]$Version = "1.0.0",
    [string]$JarPath = "target/pdv-churrasco-1.0-SNAPSHOT.jar",
    [string]$AppName = "PDVChurrasco",
    [string]$Vendor = "PDVChurrasco",
    [string]$UpgradeUuid = "7f74fe7b-b7ce-484e-819f-a31cd1b0f1be",
    [switch]$PerUserInstall
)

$ErrorActionPreference = "Stop"

function Resolve-JPackagePath {
    if ($env:JAVA_HOME) {
        $javaHomeJPackage = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
        if (Test-Path $javaHomeJPackage) {
            return $javaHomeJPackage
        }
    }

    $defaultJavaDir = "C:\Program Files\Java"
    if (Test-Path $defaultJavaDir) {
        $candidates = Get-ChildItem -Path $defaultJavaDir -Filter "jpackage.exe" -Recurse -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending

        if ($candidates) {
            return $candidates[0].FullName
        }
    }

    throw "jpackage.exe nao foi encontrado. Instale um JDK com jpackage ou defina JAVA_HOME."
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarFullPath = Join-Path $projectRoot $JarPath

if (-not (Test-Path $jarFullPath)) {
    throw "Jar nao encontrado em '$jarFullPath'. Gere o pacote Maven antes de criar o instalador."
}

$jpackagePath = Resolve-JPackagePath
$inputDir = Split-Path -Parent $jarFullPath
$jarFileName = Split-Path -Leaf $jarFullPath
$destDir = Join-Path $projectRoot "dist"

New-Item -ItemType Directory -Force -Path $destDir | Out-Null

$jpackageArgs = @(
    "--type", "msi",
    "--name", $AppName,
    "--app-version", $Version,
    "--input", $inputDir,
    "--main-jar", $jarFileName,
    "--main-class", "br.com.churrasco.Launcher",
    "--java-options", "-Dpdvchurrasco.app.version=$Version",
    "--dest", $destDir,
    "--vendor", $Vendor,
    "--description", "Sistema desktop PDV Churrasco",
    "--win-dir-chooser",
    "--win-shortcut",
    "--win-menu",
    "--win-menu-group", $AppName,
    "--win-upgrade-uuid", $UpgradeUuid
)

if ($PerUserInstall) {
    $jpackageArgs += "--win-per-user-install"
}

& $jpackagePath @jpackageArgs

Write-Host "Instalador gerado em: $destDir"
