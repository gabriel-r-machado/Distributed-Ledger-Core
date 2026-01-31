#!/bin/bash
# ============================================
# Wallet API - Repository Cleanup Script
# ============================================
# Remove temporary files and logs from repository root
# Run this BEFORE committing to GitHub

echo "🧹 Cleaning Wallet API repository..."

# Files to remove
FILES_TO_REMOVE=(
    "all_tests_final.txt"
    "app.log"
    "output.log"
    "test_final.txt"
    "test_output.txt"
    "test_output_full.txt"
    "test_results.txt"
    "test_results_final.txt"
    "test_results_final2.txt"
)

# Remove individual files
for file in "${FILES_TO_REMOVE[@]}"; do
    if [ -f "$file" ]; then
        rm -f "$file"
        echo "✅ Removed: $file"
    fi
done

# Remove directories (optional)
DIRS_TO_REMOVE=(
    ".vscode"
    "target"
)

for dir in "${DIRS_TO_REMOVE[@]}"; do
    if [ -d "$dir" ]; then
        rm -rf "$dir"
        echo "✅ Removed directory: $dir"
    fi
done

# Clean Maven build artifacts
echo ""
echo "🔨 Running Maven clean..."
if [ -f "./mvnw" ]; then
    ./mvnw clean
else
    mvn clean
fi

echo ""
echo "✨ Repository cleaned successfully!"
echo ""
echo "📁 Remaining files:"
ls -1 | grep -v "^\.git"

echo ""
echo "💡 Next steps:"
echo "  1. Review remaining files"
echo "  2. Run: ./mvnw test"
echo "  3. Commit to GitHub: git add . && git commit -m 'Clean repository for open source'"
