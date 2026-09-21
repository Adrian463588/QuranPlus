# -*- coding: utf-8 -*-
"""
Script to safely update DzikirDataCatalog.kt
"""
import re

# Read original file
with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Verify that all markers exist
assert 'id = "shalat_01"' in content, "shalat_01 not found"
assert 'id = "matsurat_01"' in content, "matsurat_01 not found"
assert 'id = "nashr_01"' in content, "nashr_01 not found"
assert 'id = "nawawi_01"' in content, "nawawi_01 not found"

print("Catalog markers verified successfully!")
