param(
    [string]$Html = "reporte_etapa.html",
    [string]$Pdf = "reporte_etapa.pdf"
)

$ErrorActionPreference = "Stop"
$raiz = $PSScriptRoot
if (-not [System.IO.Path]::IsPathRooted($Html)) { $Html = Join-Path $raiz $Html }
if (-not [System.IO.Path]::IsPathRooted($Pdf)) { $Pdf = Join-Path $raiz $Pdf }

$word = New-Object -ComObject Word.Application
$word.Visible = $false
try {
    $doc = $word.Documents.Open($Html, $false, $true)
    $doc.ExportAsFixedFormat($Pdf, 17) # 17 = PDF
    $doc.Close($false)
    Write-Host "PDF generado: $Pdf"
} finally {
    $word.Quit()
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
}
