import subprocess, collections

out = subprocess.run(
    ['git', 'log', '--no-merges', '--pretty=format:%an', '--name-only'],
    capture_output=True, text=True, encoding='utf-8', errors='replace', cwd=r'C:\Users\Jiyuu\IdeaProjects\PLANE')
lines = out.stdout.splitlines()
author = None
mods = collections.defaultdict(collections.Counter)
files = collections.defaultdict(set)
for ln in lines:
    if not ln.strip():
        continue
    if ln and not ln.startswith(' ') and '/' not in ln and '\\' not in ln:
        author = ln.strip()
        continue
    if author and ln.strip():
        f = ln.strip()
        files[author].add(f)
        if f.startswith('docs/'):
            mods[author]['文档'] += 1
        elif f.startswith('src/main/java') or f.startswith('src/test/java'):
            low = f.lower()
            if 'test' in low:
                mods[author]['测试'] += 1
            elif '/model/' in low:
                mods[author]['model'] += 1
            elif '/controller/' in low:
                mods[author]['controller'] += 1
            elif '/view/' in low:
                mods[author]['view'] += 1
            elif '/util/' in low:
                mods[author]['util'] += 1
            else:
                mods[author]['根类'] += 1
        elif f.startswith('resources') or '/pictures/' in f or '/assets/' in f or '/media/' in f:
            mods[author]['资源'] += 1
        else:
            mods[author]['其他'] += 1

for a in sorted(mods):
    print('==', a, '| 涉及文件', len(files[a]))
    for m, c in mods[a].most_common():
        print('   ', m, c)
