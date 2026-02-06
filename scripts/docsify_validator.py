import re
import sys

def validate_docsify(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    errors = []
    
    # 1. 이모티콘 검사
    emoji_pattern = re.compile(r'[\U00010000-\U0010ffff\u2600-\u27bf]')
    if emoji_pattern.search(content):
        errors.append("❌ 이모티콘 발견!")
    
    # 2. Alert 박스 검사
    if content.count('## ') > 0 and '[!' not in content:
        errors.append("❌ Alert 박스(태그) 누락!")

    return errors

if __name__ == "__main__":
    if len(sys.argv) < 2: sys.exit(1)
    errs = validate_docsify(sys.argv[1])
    if errs:
        for e in errs: print(e)
        sys.exit(1)
    else:
        print("✅ 스타일 검증 통과!")
        sys.exit(0)