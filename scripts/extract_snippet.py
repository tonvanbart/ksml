import yaml
import os
import sys

from pathlib import Path

os.makedirs('docs/_snippets', exist_ok=True)

source_dir = Path('ksml/src/test/resources/pipelines')
target_dir = Path('docs/_snippets')

pipelines = list(source_dir.glob("*.yaml"))
for definition in pipelines:
    with open(definition, 'r') as f:
        data = yaml.safe_load(f)
        partial = {"pipelines": data.get("pipelines")}
        output_path = target_dir / definition.name
        with open(output_path, 'w') as out_file:
            yaml.dump(partial, out_file, sort_keys=False)
            print(f"extracted {definition} to {output_path}")

# with open('ksml/src/test/resources/pipelines/test-aggregate-inline.yaml') as f:
#     data = yaml.safe_load(f)

# Trim or extract as needed
# partial = {"pipelines": data.get("pipelines")}
#
# with open('docs/_snippets/example.yaml', 'w') as out:
#     yaml.dump(partial, out, sort_keys=False)
