#!/usr/bin/env python3
import xml.etree.ElementTree as ET
import json
import sys

def map_priority_to_severity(priority):
    try:
        p = int(priority)
    except ValueError:
        p = 3
    if p == 1:
        return "BLOCKER"
    elif p == 2:
        return "CRITICAL"
    elif p == 3:
        return "MAJOR"
    elif p == 4:
        return "MINOR"
    else:
        return "INFO"

def convert(xml_path, json_path):
    tree = ET.parse(xml_path)
    root = tree.getroot()
    # 기본 네임스페이스를 처리하기 위한 네임스페이스 딕셔너리 정의
    ns = {'pmd': 'http://pmd.sourceforge.net/report/2.0.0'}
    issues = []
    # 네임스페이스를 고려하여 <file> 엘리먼트 찾기
    for file_elem in root.findall('pmd:file', ns):
        filename = file_elem.attrib.get('name')
        # 네임스페이스를 고려하여 <violation> 엘리먼트 찾기
        for violation in file_elem.findall('pmd:violation', ns):
            beginline = violation.attrib.get('beginline', "0")
            endline = violation.attrib.get('endline', beginline)
            rule = violation.attrib.get('rule', "unknown")
            priority = violation.attrib.get('priority', "3")
            severity = map_priority_to_severity(priority)
            message = violation.text.strip() if violation.text else ""
            issue = {
                "engineId": "pmd",
                "ruleId": rule,
                "primaryLocation": {
                    "message": message,
                    "filePath": filename,
                    "textRange": {
                        "startLine": int(beginline),
                        "endLine": int(endline)
                    }
                },
                "type": "CODE_SMELL",
                "severity": severity
            }
            issues.append(issue)
    data = {"issues": issues}
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"Converted {len(issues)} issues to JSON and saved to {json_path}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: pmd_to_sonar.py <input_xml> <output_json>")
        sys.exit(1)
    convert(sys.argv[1], sys.argv[2])

