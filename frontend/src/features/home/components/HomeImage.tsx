import type { ImgHTMLAttributes } from 'react';
import { FALLBACK_IMAGE } from '../constants/homeData';

export function HomeImage({ alt, ...props }: ImgHTMLAttributes<HTMLImageElement> & { alt: string }) {
  return (
    <img
      loading="lazy"
      decoding="async"
      alt={alt}
      onError={(event) => {
        const image = event.currentTarget;
        if (!image.src.endsWith(FALLBACK_IMAGE)) image.src = FALLBACK_IMAGE;
      }}
      {...props}
    />
  );
}
