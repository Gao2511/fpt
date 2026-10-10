(() => {
  const meta=document.querySelector('meta[name="admin-csrf"]'), token=meta?.content;
  const demo=document.querySelector('[data-demo-admin]');
  const mutation=new Set(['delete','deleteAll','deleteMultiple','lock','unlock','updateBadge']);
  function guard(form){if(form.method.toLowerCase()!=='post')return;if(demo){form.querySelectorAll('button,input[type=submit]').forEach(b=>b.disabled=true);return;}if(!form.querySelector('[name=csrf_token]')){const input=document.createElement('input');input.type='hidden';input.name='csrf_token';input.value=token||'';form.append(input);}}
  document.addEventListener('DOMContentLoaded',()=>document.querySelectorAll('form').forEach(guard));
  document.addEventListener('submit',event=>{guard(event.target);if(demo){event.preventDefault();alert('Chức năng lưu dữ liệu thật không khả dụng trong chế độ Demo.');}});
  document.addEventListener('click',event=>{const link=event.target.closest('a[href]');if(!link||event.defaultPrevented)return;const url=new URL(link.href,location.href);if(url.origin!==location.origin||!mutation.has(url.searchParams.get('action')))return;event.preventDefault();if(demo){alert('Chức năng này không khả dụng trong chế độ Demo.');return;}const form=document.createElement('form');form.method='post';form.action=url.pathname;url.searchParams.forEach((value,name)=>{const i=document.createElement('input');i.type='hidden';i.name=name;i.value=value;form.append(i);});guard(form);document.body.append(form);form.submit();});
  const original=window.fetch;
  window.fetch=(resource,options={})=>{const url=new URL(typeof resource==='string'?resource:resource.url,location.href);if(url.origin===location.origin&&url.pathname.includes('/admin/')){if(demo)return Promise.reject(new Error('Demo chỉ đọc'));options={...options};const headers=new Headers(options.headers);headers.set('X-CSRF-Token',token||'');options.headers=headers;if(mutation.has(url.searchParams.get('action'))&&(!options.method||options.method==='GET')){options.method='POST';options.body=url.searchParams;url.search='';resource=url.href;}}return original(resource,options);};
})();
