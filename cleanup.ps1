# ============================================
# Wallet API - Repository Cleanup Script
# ============================================
# Remove temporary files and logs from repository root
# Run this BEFORE committing to GitHub

Write-Host "🧹 Cleaning Wallet API repository..." -ForegroundColor Cyan

# Files to remove
$filesToRemove = @(
    "all_tests_final.txt",
    "app.log",
    "output.log",
    "test_final.txt",
    "test_output.txt",
    "test_output_full.txt",
    "test_results.txt",
    "test_results_final.txt",
    "test_results_final2.txt"
)

# Remove individual files
foreach ($file in $filesToRemove) {
    if (Test-Path $file) {
        Remove-Item $file -Force
        Write-Host "✅ Removed: $file" -ForegroundColor Green
    }
}

# Remove directories (optional)
$dirsToRemove = @(
    ".vscode",
    "target"
)

foreach ($dir in $dirsToRemove) {
    if (Test-Path $dir) {
        Remove-Item $dir -Recurse -Force
        Write-Host "✅ Removed directory: $dir" -ForegroundColor Green
    }
}

# Clean Maven build artifacts
Write-Host ""
Write-Host "🔨 Running Maven clean..." -ForegroundColor Yellow
if (Test-Path "mvnw.cmd") {
    .\mvnw.cmd clean
} else {
    mvn clean
}

Write-Host ""
Write-Host "✨ Repository cleaned successfully!" -ForegroundColor Cyan
Write-Host ""
Write-Host "📁 Remaining files:" -ForegroundColor Yellow
Get-ChildItem -Name | Where-Object { $_ -notmatch "^\.git" }

Write-Host ""
Write-Host "💡 Next steps:" -ForegroundColor Cyan
Write-Host "  1. Review remaining files"
Write-Host "  2. Run: .\mvnw.cmd test"
Write-Host "  3. Commit to GitHub: git add . && git commit -m 'Clean repository for open source'"
