$dirs = @(
    "app/src/main/java/com/safephrase",
    "app/src/main/java/com/safephrase/ui/theme",
    "app/src/main/java/com/safephrase/data/db",
    "app/src/main/java/com/safephrase/data/network",
    "app/src/main/java/com/safephrase/data/preferences",
    "app/src/main/java/com/safephrase/domain/model",
    "app/src/main/java/com/safephrase/domain/repository",
    "app/src/main/java/com/safephrase/service",
    "app/src/main/java/com/safephrase/audio",
    "app/src/main/java/com/safephrase/location",
    "app/src/main/java/com/safephrase/sync"
)

foreach ($dir in $dirs) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

New-Item -ItemType File -Force -Path "settings.gradle.kts" | Out-Null
New-Item -ItemType File -Force -Path "build.gradle.kts" | Out-Null
New-Item -ItemType File -Force -Path "gradle.properties" | Out-Null
New-Item -ItemType File -Force -Path "app/build.gradle.kts" | Out-Null
New-Item -ItemType File -Force -Path "app/src/main/AndroidManifest.xml" | Out-Null

Write-Host "Scaffolding complete."
