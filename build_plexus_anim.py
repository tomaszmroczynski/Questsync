import json, struct, math, random
random.seed(13)

def build_glb(path, verts, tris, color):
    buf=bytearray()
    idx=bytearray()
    for (a,b,c) in tris: idx+=struct.pack("<III",a,b,c)
    while len(buf)%4: buf.append(0)
    io=len(buf); buf+=idx
    while len(buf)%4: buf.append(0)
    po=len(buf); mn=[1e9]*3; mx=[-1e9]*3
    for (x,y,z) in verts:
        buf+=struct.pack("<fff",x,y,z)
        for k,c in enumerate((x,y,z)): mn[k]=min(mn[k],c); mx[k]=max(mx[k],c)
    while len(buf)%4: buf.append(0)
    g={"asset":{"version":"2.0"},"extensionsUsed":["KHR_materials_unlit"],"scene":0,"scenes":[{"nodes":[0]}],
       "nodes":[{"mesh":0}],"meshes":[{"primitives":[{"attributes":{"POSITION":1},"indices":0,"material":0,"mode":4}]}],
       "materials":[{"pbrMetallicRoughness":{"baseColorFactor":color,"metallicFactor":0.0,"roughnessFactor":1.0},
                     "emissiveFactor":color[:3],"alphaMode":"BLEND","doubleSided":True,"extensions":{"KHR_materials_unlit":{}}}],
       "bufferViews":[{"buffer":0,"byteOffset":io,"byteLength":len(idx),"target":34963},
                      {"buffer":0,"byteOffset":po,"byteLength":len(buf)-po,"target":34964}],
       "accessors":[{"bufferView":0,"componentType":5125,"count":len(tris)*3,"type":"SCALAR"},
                    {"bufferView":1,"componentType":5126,"count":len(verts),"type":"VEC3","min":mn,"max":mx}],
       "buffers":[{"byteLength":len(buf)}]}
    jb=json.dumps(g,separators=(',',':')).encode()
    while len(jb)%4: jb+=b' '
    total=12+8+len(jb)+8+len(buf); out=bytearray()
    out+=struct.pack("<III",0x46546C67,2,total)+struct.pack("<II",len(jb),0x4E4F534A)+jb+struct.pack("<II",len(buf),0x004E4942)+buf
    open(path,"wb").write(out); print("wrote",path,len(out),"verts",len(verts))

def octa(cx,cy,cz,s):
    v=[(cx+s,cy,cz),(cx-s,cy,cz),(cx,cy+s,cz),(cx,cy-s,cz),(cx,cy,cz+s),(cx,cy,cz-s)]
    f=[(0,2,4),(2,1,4),(1,3,4),(3,0,4),(2,0,5),(1,2,5),(3,1,5),(0,3,5)]
    return v,f
def add(vt,tr,v,f):
    b=len(vt); vt.extend(v)
    for (a,bb,c) in f: tr.append((b+a,b+bb,b+c))
def box_edge(a,bp,w):
    d=(bp[0]-a[0],bp[1]-a[1],bp[2]-a[2]); L=math.sqrt(sum(c*c for c in d))
    if L<1e-6: return None
    d=(d[0]/L,d[1]/L,d[2]/L)
    def cr(p,q): return (p[1]*q[2]-p[2]*q[1],p[2]*q[0]-p[0]*q[2],p[0]*q[1]-p[1]*q[0])
    def nr(p):
        l=math.sqrt(sum(c*c for c in p)) or 1.0; return (p[0]/l,p[1]/l,p[2]/l)
    rv=(0,1,0) if abs(d[1])<0.9 else (1,0,0)
    u=nr(cr(d,rv)); v=nr(cr(d,u))
    def co(p,su,sv): return (p[0]+u[0]*su*w+v[0]*sv*w,p[1]+u[1]*su*w+v[1]*sv*w,p[2]+u[2]*su*w+v[2]*sv*w)
    c=[co(a,1,1),co(a,1,-1),co(a,-1,-1),co(a,-1,1),co(bp,1,1),co(bp,1,-1),co(bp,-1,-1),co(bp,-1,1)]
    f=[(0,1,5),(0,5,4),(1,2,6),(1,6,5),(2,3,7),(2,7,6),(3,0,4),(3,4,7),(0,3,2),(0,2,1),(4,5,6),(4,6,7)]
    return c,f

src=open("design/human-points.js",encoding="utf-8").read()
pts=json.loads(src[src.index("["):src.rindex("]")+1])
# edges
R=0.06;K=3;cell=R;grid={}
def key(p): return (int(p[0]//cell),int(p[1]//cell),int(p[2]//cell))
for i,p in enumerate(pts): grid.setdefault(key(p),[]).append(i)
def d2(a,b): return sum((a[k]-b[k])**2 for k in range(3))
edges=set()
for i,p in enumerate(pts):
    kx,ky,kz=key(p); cand=[]
    for ax in(-1,0,1):
        for ay in(-1,0,1):
            for az in(-1,0,1):
                for j in grid.get((kx+ax,ky+ay,kz+az),[]):
                    if j!=i and d2(p,pts[j])<=R*R: cand.append((d2(p,pts[j]),j))
    cand.sort()
    for _,j in cand[:K]: edges.add((min(i,j),max(i,j)))

# 3 node groups (smaller dots)
groups=[([],[]),([],[]),([],[])]
gsize=[0.0034,0.0040,0.0048]
for (x,y,z) in pts:
    gi=random.choice([0,0,1,1,2])
    add(groups[gi][0],groups[gi][1],*octa(x,y,z,gsize[gi]))
build_glb("app/src/main/assets/plexus_a.glb",groups[0][0],groups[0][1],[1.0,0.66,0.32,0.85])
build_glb("app/src/main/assets/plexus_b.glb",groups[1][0],groups[1][1],[1.0,0.72,0.40,0.85])
build_glb("app/src/main/assets/plexus_c.glb",groups[2][0],groups[2][1],[1.0,0.88,0.6,0.9])
# lines
lv=[];lf=[]
for (i,j) in edges:
    r=box_edge(pts[i],pts[j],0.00028)
    if r: add(lv,lf,r[0],r[1])
build_glb("app/src/main/assets/plexus_lines.glb",lv,lf,[0.95,0.6,0.3,0.30])
print("edges",len(edges))
