import sys

def check_loss(original_path, formatted_path):
    orig = open(original_path, 'r').read()
    form = open(formatted_path, 'r').read()
    
    orig_lines = len(orig.splitlines())
    form_lines = len(form.splitlines())
    
    print(f"Original lines: {orig_lines}, Formatted lines: {form_lines}")
    
    # 1. 라인 수 대조 (최소 90% 이상 유지)
    if form_lines < orig_lines * 0.9:
        print("❌ 데이터 유실 의심: 라인 수가 너무 많이 줄어들었습니다!")
        return False
        
    # 2. 코드 블록 개수 대조
    if orig.count('```') != form.count('```'):
        print("❌ 코드 블록 개수 불일치! 데이터 손상 위험.")
        return False
        
    return True

if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(1)
    if not check_loss(sys.argv[1], sys.argv[2]):
        sys.exit(1)
    else:
        print("✅ 데이터 무결성 검증 통과!")
        sys.exit(0)
