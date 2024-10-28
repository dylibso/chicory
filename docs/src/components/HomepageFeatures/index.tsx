import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
  title: string;
  Svg: React.ComponentType<React.ComponentProps<'svg'>>;
  description: JSX.Element;
};

const FeatureList: FeatureItem[] = [
  {
    title: 'Pure Java',
    Svg: require('@site/static/img/zero-deps.svg').default,
    description: (
      <>
        Using Chicory you don't need to rely on any system resource.
        Everything runs in 100% pure Java on top of the standard library.
      </>
    ),
  },
  {
    title: 'Easy integration',
    Svg: require('@site/static/img/wrench.svg').default,
    description: (
      <>
        Integrating Chicory in your project is smooth and only requires a few steps.
        Give your application a twist with a plugin system.
      </>
    ),
  },
  {
    title: 'Secure by design',
    Svg: require('@site/static/img/helmet.svg').default,
    description: (
      <>
        Web Assembly modules are running in a sandboxed environment.
        You have full control over the used resources.
      </>
    ),
  }
];

function Feature({title, Svg, description}: FeatureItem) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
      </div>
      <div className="text--center padding-horiz--md">
        <Heading as="h3">{title}</Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): JSX.Element {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
