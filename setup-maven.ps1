findstr /S /I /N /C:"admin" backend\src\main\*.java backend\src\main\*.sql$ErrorActionPreference = 'Stop'
$tools = Join-Path $PSScriptRoot '.tools'
New-Item -ItemType Directory -Force $tools | Out-Null
Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip' -OutFile (Join-Path $tools 'maven.zip')
Expand-Archive -LiteralPath (Join-Path $tools 'maven.zip') -DestinationPath $tools -Force
