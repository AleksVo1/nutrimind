"""Copy Allure results for public Pages without Playwright session traces."""
from pathlib import Path
from allure_archive import public_results

public_results(Path("target/allure-results"), Path("target/allure-pages-results"))
print("Public Allure results prepared without session trace attachments")
